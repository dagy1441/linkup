package com.siide.linkup.core.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.siide.linkup.core.idempotency.exception.IdempotencyInProgressException;
import com.siide.linkup.core.idempotency.exception.IdempotencyKeyInvalidException;
import com.siide.linkup.core.idempotency.exception.IdempotencyKeyReusedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IdempotencyServiceTest {

    private IdempotencyKeyRepository repository;
    private IdempotencyService service;

    private final Instant now = Instant.parse("2026-01-01T10:00:00Z");
    private final UUID userId = UUID.randomUUID();
    private final String endpoint = "POST /api/v1/bookings";
    private final String key = "abc-123";

    @BeforeEach
    void setUp() {
        repository = mock(IdempotencyKeyRepository.class);
        java.util.Map<UUID, IdempotencyKey> store = new java.util.concurrent.ConcurrentHashMap<>();
        when(repository.saveAndFlush(any(IdempotencyKey.class))).thenAnswer(inv -> {
            IdempotencyKey row = inv.getArgument(0);
            store.put(row.getId(), row);
            return row;
        });
        when(repository.save(any(IdempotencyKey.class))).thenAnswer(inv -> {
            IdempotencyKey row = inv.getArgument(0);
            store.put(row.getId(), row);
            return row;
        });
        when(repository.findById(any(UUID.class))).thenAnswer(inv -> Optional.ofNullable(store.get(inv.getArgument(0))));
        TransactionTemplate inlineTx = new TransactionTemplate() {
            @Override
            public <T> T execute(TransactionCallback<T> action) {
                return action.doInTransaction(null);
            }

            @Override
            public void executeWithoutResult(java.util.function.Consumer<org.springframework.transaction.TransactionStatus> action) {
                action.accept(null);
            }
        };
        ObjectMapper mapper = new ObjectMapper();
        IdempotencyProperties props = new IdempotencyProperties("Idempotency-Key",
                Duration.ofHours(24), 128, "0 0 3 * * *");
        service = new IdempotencyService(repository, mapper, props, inlineTx,
                Clock.fixed(now, ZoneOffset.UTC));
    }

    @Test
    void execute_runs_handler_and_caches_response_on_first_call() {
        when(repository.findByKeyAndUserIdAndEndpoint(key, userId, endpoint)).thenReturn(Optional.empty());

        ResponseEntity<String> response = service.execute(key, userId, endpoint, body(1), String.class,
                () -> ResponseEntity.status(HttpStatus.CREATED).body("ok"));

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isEqualTo("ok");
        verify(repository).saveAndFlush(any(IdempotencyKey.class));
    }

    @Test
    void execute_replays_cached_response_when_hash_matches() {
        IdempotencyKey cached = new IdempotencyKey(key, userId, endpoint,
                sha256OfBody(1), now.minus(Duration.ofMinutes(5)), now.plus(Duration.ofHours(23)));
        cached.completeWith(201, "\"ok\"", String.class.getName());
        when(repository.findByKeyAndUserIdAndEndpoint(key, userId, endpoint)).thenReturn(Optional.of(cached));

        AtomicInteger handlerInvocations = new AtomicInteger();
        ResponseEntity<String> response = service.execute(key, userId, endpoint, body(1), String.class, () -> {
            handlerInvocations.incrementAndGet();
            return ResponseEntity.ok("should-not-run");
        });

        assertThat(handlerInvocations.get()).isZero();
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isEqualTo("ok");
    }

    @Test
    void execute_throws_reused_exception_when_hash_differs() {
        IdempotencyKey cached = new IdempotencyKey(key, userId, endpoint,
                sha256OfBody(1), now, now.plus(Duration.ofHours(23)));
        cached.completeWith(201, "\"ok\"", String.class.getName());
        when(repository.findByKeyAndUserIdAndEndpoint(key, userId, endpoint)).thenReturn(Optional.of(cached));

        assertThatThrownBy(() -> service.execute(key, userId, endpoint, body(2), String.class,
                () -> ResponseEntity.ok("never")))
                .isInstanceOf(IdempotencyKeyReusedException.class);
    }

    @Test
    void execute_throws_in_progress_when_cached_row_is_pending() {
        IdempotencyKey pending = new IdempotencyKey(key, userId, endpoint,
                sha256OfBody(1), now, now.plus(Duration.ofHours(23)));
        when(repository.findByKeyAndUserIdAndEndpoint(key, userId, endpoint)).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> service.execute(key, userId, endpoint, body(1), String.class,
                () -> ResponseEntity.ok("never")))
                .isInstanceOf(IdempotencyInProgressException.class);
    }

    @Test
    void execute_rejects_blank_key() {
        assertThatThrownBy(() -> service.execute(" ", userId, endpoint, body(1), String.class,
                () -> ResponseEntity.ok("nope")))
                .isInstanceOf(IdempotencyKeyInvalidException.class);
    }

    @Test
    void execute_rejects_oversized_key() {
        String huge = "x".repeat(200);
        assertThatThrownBy(() -> service.execute(huge, userId, endpoint, body(1), String.class,
                () -> ResponseEntity.ok("nope")))
                .isInstanceOf(IdempotencyKeyInvalidException.class);
    }

    @Test
    void execute_deletes_row_when_handler_throws_so_retry_can_succeed() {
        when(repository.findByKeyAndUserIdAndEndpoint(key, userId, endpoint)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(key, userId, endpoint, body(1), String.class,
                (Supplier<ResponseEntity<String>>) () -> { throw new IllegalStateException("boom"); }))
                .isInstanceOf(IllegalStateException.class);

        verify(repository).deleteById(any(UUID.class));
    }

    @Test
    void execute_falls_through_when_existing_row_is_expired() {
        IdempotencyKey expired = new IdempotencyKey(key, userId, endpoint,
                sha256OfBody(1), now.minus(Duration.ofDays(2)), now.minus(Duration.ofHours(1)));
        expired.completeWith(201, "\"old\"", String.class.getName());
        when(repository.findByKeyAndUserIdAndEndpoint(key, userId, endpoint)).thenReturn(Optional.of(expired));

        AtomicInteger handlerInvocations = new AtomicInteger();
        ResponseEntity<String> response = service.execute(key, userId, endpoint, body(1), String.class, () -> {
            handlerInvocations.incrementAndGet();
            return ResponseEntity.status(HttpStatus.CREATED).body("fresh");
        });

        assertThat(handlerInvocations.get()).isEqualTo(1);
        assertThat(response.getBody()).isEqualTo("fresh");
        verify(repository).deleteById(expired.getId());
    }

    private static java.util.Map<String, Integer> body(int n) {
        return java.util.Map.of("v", n);
    }

    private String sha256OfBody(int n) {
        try {
            ObjectMapper m = new ObjectMapper();
            String json = m.writeValueAsString(body(n));
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            return java.util.HexFormat.of().formatHex(md.digest(json.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}
