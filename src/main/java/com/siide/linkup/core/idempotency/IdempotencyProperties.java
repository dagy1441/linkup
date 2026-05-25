package com.siide.linkup.core.idempotency;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Idempotency configuration bound from {@code linkup.idempotency.*}.
 *
 * @param headerName     HTTP header carrying the client-generated key. Default {@code Idempotency-Key}.
 * @param ttl            how long a completed entry stays in the cache. Default 24h.
 * @param maxKeyLength   hard upper bound on the header value length (rejection at 128).
 * @param maxBodyBytes   upper bound on the serialized request body fed to SHA-256.
 *                       Above this size we reject the request — protects the CPU
 *                       from oversized payloads. Default 64 KB.
 * @param cleanupCron    cron expression for the TTL sweep. Default 3am daily.
 */
@ConfigurationProperties(prefix = "linkup.idempotency")
public record IdempotencyProperties(
        String headerName,
        Duration ttl,
        int maxKeyLength,
        int maxBodyBytes,
        String cleanupCron
) {
    public IdempotencyProperties {
        if (headerName == null || headerName.isBlank()) headerName = "Idempotency-Key";
        if (ttl == null || ttl.isZero() || ttl.isNegative()) ttl = Duration.ofHours(24);
        if (maxKeyLength <= 0) maxKeyLength = 128;
        if (maxBodyBytes <= 0) maxBodyBytes = 64 * 1024;
        if (cleanupCron == null || cleanupCron.isBlank()) cleanupCron = "0 0 3 * * *";
    }
}
