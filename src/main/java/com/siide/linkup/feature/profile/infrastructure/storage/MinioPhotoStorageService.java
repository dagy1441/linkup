package com.siide.linkup.feature.profile.infrastructure.storage;

import com.siide.linkup.feature.profile.application.ProfileStorageProperties;
import com.siide.linkup.feature.profile.domain.storage.PhotoStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.InputStream;
import java.time.Duration;
import java.util.UUID;

/**
 * S3-compatible adapter for {@link PhotoStorageService}. Works against MinIO in
 * dev and AWS S3 in prod (no code change — just env vars). Path layout:
 * {@code profiles/<profileId>/avatar.<ext>} — overwrite-on-reupload so the old
 * key is naturally orphaned.
 */
@Service
public class MinioPhotoStorageService implements PhotoStorageService {

    private static final Logger log = LoggerFactory.getLogger(MinioPhotoStorageService.class);

    private final S3Client client;
    private final S3Presigner presigner;
    private final String bucket;

    public MinioPhotoStorageService(S3Client client, S3Presigner presigner, ProfileStorageProperties properties) {
        this.client = client;
        this.presigner = presigner;
        this.bucket = properties.storage().bucket();
    }

    @Override
    public String upload(UUID profileId, String contentType, long size, InputStream data) {
        String key = "profiles/" + profileId + "/avatar" + extensionFor(contentType);
        try {
            client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(contentType)
                            .contentLength(size)
                            .build(),
                    RequestBody.fromInputStream(data, size));
            log.debug("Uploaded photo key={} size={} contentType={}", key, size, contentType);
            return key;
        } catch (RuntimeException e) {
            throw new IllegalStateException("Failed to upload photo for profile " + profileId, e);
        }
    }

    @Override
    public void delete(String key) {
        if (key == null || key.isBlank()) return;
        try {
            client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
            log.debug("Deleted photo key={}", key);
        } catch (NoSuchKeyException notFound) {
            // Idempotent: deleting a missing object is fine.
            log.debug("Photo key={} already absent", key);
        } catch (RuntimeException e) {
            throw new IllegalStateException("Failed to delete photo " + key, e);
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
            throw new IllegalStateException("Failed to presign photo " + key, e);
        }
    }

    /** Derive a file extension from MIME — keeps URLs human-friendly for ops. */
    private static String extensionFor(String contentType) {
        return switch (contentType.toLowerCase()) {
            case "image/jpeg" -> ".jpg";
            case "image/png"  -> ".png";
            case "image/webp" -> ".webp";
            default           -> ".bin";
        };
    }
}
