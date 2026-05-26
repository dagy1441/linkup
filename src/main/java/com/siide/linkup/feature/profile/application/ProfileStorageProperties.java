package com.siide.linkup.feature.profile.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;
import java.util.Set;

/**
 * Configuration for the profile module's photo upload feature and the
 * S3-compatible storage backend.
 *
 * @param maxBytes              upper bound on uploaded photo size (defensive cap)
 * @param allowedContentTypes   MIME whitelist (no SVG → XSS vector, no GIF → animated)
 * @param storage               storage backend tuning
 */
@ConfigurationProperties(prefix = "linkup.profile")
public record ProfileStorageProperties(
        long maxBytes,
        List<String> allowedContentTypes,
        Storage storage
) {

    public ProfileStorageProperties {
        if (maxBytes <= 0) maxBytes = 1_048_576L; // 1 MB
        if (allowedContentTypes == null || allowedContentTypes.isEmpty()) {
            allowedContentTypes = List.of("image/jpeg", "image/png", "image/webp");
        }
        if (storage == null) {
            storage = new Storage("http://localhost:9000", "minioadmin", "minioadmin",
                    "linkup-profiles", Duration.ofHours(1));
        }
    }

    public Set<String> allowedContentTypeSet() {
        return Set.copyOf(allowedContentTypes);
    }

    /**
     * S3-compatible client settings. Defaults match the dev MinIO container in
     * {@code docker-compose.yaml}. Prod overrides via env vars
     * ({@code LINKUP_PROFILE_STORAGE_ENDPOINT}, etc).
     */
    public record Storage(
            String endpoint,
            String accessKey,
            String secretKey,
            String bucket,
            Duration presignedUrlTtl
    ) {
        public Storage {
            if (endpoint == null || endpoint.isBlank()) endpoint = "http://localhost:9000";
            if (accessKey == null || accessKey.isBlank()) accessKey = "minioadmin";
            if (secretKey == null || secretKey.isBlank()) secretKey = "minioadmin";
            if (bucket == null || bucket.isBlank()) bucket = "linkup-profiles";
            if (presignedUrlTtl == null || presignedUrlTtl.isZero() || presignedUrlTtl.isNegative()) {
                presignedUrlTtl = Duration.ofHours(1);
            }
        }
    }
}
