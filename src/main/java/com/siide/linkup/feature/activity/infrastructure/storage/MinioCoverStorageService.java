package com.siide.linkup.feature.activity.infrastructure.storage;

import com.siide.linkup.feature.activity.application.ActivityCoverProperties;
import com.siide.linkup.feature.activity.domain.storage.CoverStorageService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.InputStream;
import java.time.Duration;
import java.util.UUID;

/**
 * S3-compatible adapter for {@link CoverStorageService}. Works against MinIO
 * in dev and AWS S3 in prod (no code change — just env vars). Path layout:
 * {@code activities/<activityId>/cover.<ext>} — overwrite-on-reupload so
 * the orphan key is naturally collected.
 * <p>
 * Reuses the {@link S3Client} + {@link S3Presigner} beans that the profile
 * module provides (see {@code feature.profile.infrastructure.storage.MinioConfig});
 * we only own the bucket-existence guarantee + the per-feature path layout.
 */
@Service
public class MinioCoverStorageService implements CoverStorageService {

    private static final Logger log = LoggerFactory.getLogger(MinioCoverStorageService.class);

    private final S3Client client;
    private final S3Presigner presigner;
    private final String bucket;

    public MinioCoverStorageService(S3Client client, S3Presigner presigner, ActivityCoverProperties properties) {
        this.client = client;
        this.presigner = presigner;
        this.bucket = properties.bucket();
    }

    @PostConstruct
    public void ensureBucket() {
        try {
            client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
            log.debug("S3 bucket '{}' already exists (activity covers)", bucket);
        } catch (NoSuchBucketException missing) {
            try {
                client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
                log.info("Created S3 bucket '{}' (activity covers)", bucket);
            } catch (BucketAlreadyOwnedByYouException raceLost) {
                log.debug("S3 bucket '{}' created concurrently", bucket);
            }
        }
    }

    @Override
    public String upload(UUID activityId, String contentType, long size, InputStream data) {
        String key = "activities/" + activityId + "/cover" + extensionFor(contentType);
        try {
            client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(contentType)
                            .contentLength(size)
                            .build(),
                    RequestBody.fromInputStream(data, size));
            log.debug("Uploaded cover key={} size={} contentType={}", key, size, contentType);
            return key;
        } catch (RuntimeException e) {
            throw new IllegalStateException("Failed to upload cover for activity " + activityId, e);
        }
    }

    @Override
    public void delete(String key) {
        if (key == null || key.isBlank()) return;
        try {
            client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
            log.debug("Deleted cover key={}", key);
        } catch (NoSuchKeyException notFound) {
            log.debug("Cover key={} already absent", key);
        } catch (RuntimeException e) {
            throw new IllegalStateException("Failed to delete cover " + key, e);
        }
    }

    @Override
    public String presignedUrl(String key, Duration ttl) {
        if (key == null || key.isBlank()) return null;
        try {
            return presigner.presignGetObject(GetObjectPresignRequest.builder()
                            .signatureDuration(ttl)
                            .getObjectRequest(GetObjectRequest.builder().bucket(bucket).key(key).build())
                            .build())
                    .url()
                    .toString();
        } catch (RuntimeException e) {
            throw new IllegalStateException("Failed to presign cover " + key, e);
        }
    }

    private static String extensionFor(String contentType) {
        return switch (contentType.toLowerCase()) {
            case "image/jpeg" -> ".jpg";
            case "image/png"  -> ".png";
            case "image/webp" -> ".webp";
            default           -> ".bin";
        };
    }
}
