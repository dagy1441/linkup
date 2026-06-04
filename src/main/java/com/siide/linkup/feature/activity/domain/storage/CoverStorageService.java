package com.siide.linkup.feature.activity.domain.storage;

import java.io.InputStream;
import java.time.Duration;
import java.util.UUID;

/**
 * Port for activity cover-image storage. The domain owns the contract;
 * infrastructure picks the backend (MinIO in dev, S3 in prod). Mirrors
 * {@code feature.profile.domain.storage.PhotoStorageService} but stays
 * isolated so each feature can evolve its own ACL / retention policy.
 */
public interface CoverStorageService {

    /**
     * Upload a cover for the given {@code activityId}. Returns the storage key
     * that must be stored on the aggregate. Implementations MUST be idempotent
     * on the {@code (activityId, contentType)} pair — a re-upload overwrites
     * the previous cover.
     *
     * @param activityId  target activity (drives the storage path)
     * @param contentType MIME (image/jpeg, image/png, image/webp). Stored as metadata.
     * @param size        exact length in bytes (the SDK needs it upfront)
     * @param data        the raw bytes (closed by the implementation)
     * @return the storage key (e.g. {@code activities/<uuid>/cover.jpg})
     */
    String upload(UUID activityId, String contentType, long size, InputStream data);

    /** Delete the object behind a previously returned key. No-op if it's already gone. */
    void delete(String key);

    /**
     * Resolve a key to a time-limited read URL for the caller's browser. Returns
     * {@code null} when {@code key} is {@code null} (activity has no cover).
     */
    String presignedUrl(String key, Duration ttl);
}
