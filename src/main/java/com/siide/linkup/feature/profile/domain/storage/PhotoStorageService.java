package com.siide.linkup.feature.profile.domain.storage;

import java.io.InputStream;
import java.time.Duration;

/**
 * Port for profile photo storage. The domain owns the contract; infrastructure
 * picks the backend (MinIO in dev, S3 in prod). Returning an opaque {@code String}
 * key keeps the aggregate persistence-agnostic — the controller resolves it to
 * a presigned URL on the way out.
 */
public interface PhotoStorageService {

    /**
     * Upload a photo for the given {@code profileId}. Returns the storage key that
     * must be stored on the aggregate. Implementations MUST be idempotent on the
     * (profileId, contentType) pair — a re-upload overwrites the previous photo.
     *
     * @param profileId   target profile (drives the storage path)
     * @param contentType MIME (image/jpeg, image/png, image/webp). Stored as metadata.
     * @param size        exact length in bytes (the SDK needs it upfront)
     * @param data        the raw bytes (closed by the implementation)
     * @return the storage key (e.g. {@code profiles/<uuid>/avatar.jpg})
     */
    String upload(java.util.UUID profileId, String contentType, long size, InputStream data);

    /** Delete the object behind a previously returned key. No-op if it's already gone. */
    void delete(String key);

    /**
     * Resolve a key to a time-limited read URL for the caller's browser. Returns
     * {@code null} when {@code key} is {@code null} (profile has no photo).
     */
    String presignedUrl(String key, Duration ttl);
}
