package com.siide.linkup.core.idempotency;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

/**
 * Replay-cache row for an authenticated request to a mutating endpoint.
 * <p>
 * Lifecycle:
 * <ol>
 *     <li>Insert with {@code responseBody == null} when execution starts.</li>
 *     <li>Update with status + serialized body on successful completion.</li>
 *     <li>Deleted on handler exception so a retry can re-attempt.</li>
 *     <li>Deleted by {@link IdempotencyCleanupScheduler} when {@code expires_at < now}.</li>
 * </ol>
 */
@Entity
@Table(
        name = "idempotency_keys",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_idempotency_keys_key_user_endpoint",
                columnNames = {"idem_key", "user_id", "endpoint"})
)
public class IdempotencyKey {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "idem_key", nullable = false, length = 128, updatable = false)
    private String key;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "endpoint", nullable = false, length = 255, updatable = false)
    private String endpoint;

    @Column(name = "request_hash", nullable = false, length = 64, updatable = false)
    private String requestHash;

    @Column(name = "response_status")
    private Integer responseStatus;

    @Column(name = "response_body", columnDefinition = "text")
    private String responseBody;

    @Column(name = "response_type", length = 255)
    private String responseType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected IdempotencyKey() {
        // JPA
    }

    public IdempotencyKey(String key, UUID userId, String endpoint, String requestHash,
                          Instant createdAt, Instant expiresAt) {
        this.id = UUID.randomUUID();
        this.key = key;
        this.userId = userId;
        this.endpoint = endpoint;
        this.requestHash = requestHash;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public void completeWith(int status, String body, String type) {
        this.responseStatus = status;
        this.responseBody = body;
        this.responseType = type;
    }

    public boolean isPending() {
        return responseStatus == null;
    }

    public boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }

    public UUID getId() { return id; }
    public String getKey() { return key; }
    public UUID getUserId() { return userId; }
    public String getEndpoint() { return endpoint; }
    public String getRequestHash() { return requestHash; }
    public Integer getResponseStatus() { return responseStatus; }
    public String getResponseBody() { return responseBody; }
    public String getResponseType() { return responseType; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
}
