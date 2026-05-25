package com.siide.linkup.core.idempotency;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.siide.linkup.core.idempotency.exception.IdempotencyInProgressException;
import com.siide.linkup.core.idempotency.exception.IdempotencyKeyInvalidException;
import com.siide.linkup.core.idempotency.exception.IdempotencyKeyReusedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Replay-cache for mutating endpoints, persisted in {@code idempotency_keys}.
 * <p>
 * Algorithm:
 * <ol>
 *     <li>Look up an existing row for {@code (key, userId, endpoint)}.</li>
 *     <li>If found and expired: delete it and fall through.</li>
 *     <li>If found and body hash differs: reject (422).</li>
 *     <li>If found and still pending (no response yet): reject (409).</li>
 *     <li>If found and completed: deserialize the cached response and return it.</li>
 *     <li>Otherwise insert a pending row, execute the supplier, and update the row with
 *         the response. On exception, delete the row so a retry can re-attempt.</li>
 * </ol>
 */
@Service
public class IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);

    private final IdempotencyKeyRepository repository;
    private final ObjectMapper objectMapper;
    private final IdempotencyProperties properties;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public IdempotencyService(IdempotencyKeyRepository repository,
                              ObjectMapper objectMapper,
                              IdempotencyProperties properties,
                              TransactionTemplate transactionTemplate,
                              Clock clock) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
    }

    public <T> ResponseEntity<T> execute(String key,
                                         UUID userId,
                                         String endpoint,
                                         Object requestBody,
                                         Class<T> responseType,
                                         Supplier<ResponseEntity<T>> handler) {
        validateKey(key);
        String hash = sha256(serialize(requestBody));
        Instant now = Instant.now(clock);

        Optional<IdempotencyKey> existing = repository.findByKeyAndUserIdAndEndpoint(key, userId, endpoint);
        if (existing.isPresent()) {
            IdempotencyKey row = existing.get();
            if (row.isExpired(now)) {
                deleteInNewTx(row.getId());
            } else if (!row.getRequestHash().equals(hash)) {
                throw new IdempotencyKeyReusedException(key);
            } else if (row.isPending()) {
                throw new IdempotencyInProgressException(key);
            } else {
                log.debug("Idempotency replay key={} endpoint={}", key, endpoint);
                return replayCachedResponse(row, responseType);
            }
        }

        IdempotencyKey pending = new IdempotencyKey(key, userId, endpoint, hash, now,
                now.plus(properties.ttl()));
        try {
            insertInNewTx(pending);
        } catch (DataIntegrityViolationException race) {
            // Lost the insert race: another thread just inserted the same key.
            IdempotencyKey winner = repository
                    .findByKeyAndUserIdAndEndpoint(key, userId, endpoint)
                    .orElseThrow(() -> race);
            if (!winner.getRequestHash().equals(hash)) {
                throw new IdempotencyKeyReusedException(key);
            }
            if (winner.isPending()) {
                throw new IdempotencyInProgressException(key);
            }
            return replayCachedResponse(winner, responseType);
        }

        ResponseEntity<T> response;
        try {
            response = handler.get();
        } catch (RuntimeException businessError) {
            // Handler failed — delete the pending row so a retry can re-attempt.
            deleteInNewTx(pending.getId());
            throw businessError;
        }
        // Handler succeeded. Caching the response is best-effort: if the completion
        // update fails (DB blip, deadlock), we intentionally KEEP the pending row.
        // - The side effects of the handler are already committed.
        // - Any retry with the same key will get 409 IN_PROGRESS until TTL expires.
        // - The alternative (delete on cache failure) would let the caller replay
        //   the request and cause a second execution — way worse than a 409.
        try {
            completeInNewTx(pending.getId(), response);
        } catch (RuntimeException cacheError) {
            log.error("Failed to cache idempotency response key={} endpoint={} — handler succeeded, "
                    + "pending row retained to block retries until TTL", key, endpoint, cacheError);
        }
        return response;
    }

    private void validateKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IdempotencyKeyInvalidException("header is required");
        }
        if (key.length() > properties.maxKeyLength()) {
            throw new IdempotencyKeyInvalidException("length exceeds " + properties.maxKeyLength());
        }
    }

    private String serialize(Object body) {
        if (body == null) return "null";
        try {
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize idempotency request body", e);
        }
    }

    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private <T> ResponseEntity<T> replayCachedResponse(IdempotencyKey row, Class<T> responseType) {
        try {
            T body = row.getResponseBody() == null
                    ? null
                    : objectMapper.readValue(row.getResponseBody(), responseType);
            return ResponseEntity
                    .status(HttpStatus.valueOf(row.getResponseStatus()))
                    .body(body);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize cached idempotency response", e);
        }
    }

    private void insertInNewTx(IdempotencyKey row) {
        transactionTemplate.executeWithoutResult(status -> repository.saveAndFlush(row));
    }

    private <T> void completeInNewTx(UUID id, ResponseEntity<T> response) {
        transactionTemplate.executeWithoutResult(status -> {
            IdempotencyKey row = repository.findById(id).orElseThrow();
            String body = response.hasBody() ? serialize(response.getBody()) : null;
            String type = response.hasBody() ? response.getBody().getClass().getName() : null;
            row.completeWith(response.getStatusCode().value(), body, type);
            repository.save(row);
        });
    }

    private void deleteInNewTx(UUID id) {
        transactionTemplate.executeWithoutResult(status -> repository.deleteById(id));
    }
}
