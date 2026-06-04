package com.siide.linkup.feature.activity.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;
import java.util.Set;

/**
 * Configuration for the activity cover upload feature. Connection details
 * (S3 endpoint, access key, secret key) are shared with the profile module
 * via the {@code S3Client} + {@code S3Presigner} beans that
 * {@code feature.profile.infrastructure.storage.MinioConfig} provides;
 * we only declare the per-feature knobs here.
 *
 * @param maxBytes              upper bound on uploaded cover size (defensive cap)
 * @param allowedContentTypes   MIME whitelist
 * @param bucket                S3 bucket dedicated to activity covers
 * @param presignedUrlTtl       lifetime of the URLs returned to the browser
 */
@ConfigurationProperties(prefix = "linkup.activity.cover")
public record ActivityCoverProperties(
        long maxBytes,
        List<String> allowedContentTypes,
        String bucket,
        Duration presignedUrlTtl
) {

    public ActivityCoverProperties {
        if (maxBytes <= 0) maxBytes = 2L * 1024 * 1024; // 2 MB — covers are visual, allow slightly more than avatars
        if (allowedContentTypes == null || allowedContentTypes.isEmpty()) {
            allowedContentTypes = List.of("image/jpeg", "image/png", "image/webp");
        }
        if (bucket == null || bucket.isBlank()) bucket = "linkup-activity-covers";
        if (presignedUrlTtl == null || presignedUrlTtl.isZero() || presignedUrlTtl.isNegative()) {
            presignedUrlTtl = Duration.ofHours(1);
        }
    }

    public Set<String> allowedContentTypeSet() {
        return Set.copyOf(allowedContentTypes);
    }
}
