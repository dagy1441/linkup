package com.siide.linkup.core.exception;

import java.time.Instant;
import java.util.List;

/**
 * Standardized error payload returned by the API.
 *
 * @param timestamp   server-side instant the error was produced
 * @param status      HTTP status code
 * @param code        stable error code (e.g. {@code ACTIVITY_NOT_FOUND})
 * @param message     human-readable description
 * @param path        request URI that caused the error
 * @param fieldErrors per-field validation errors (nullable)
 */
public record ApiError(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path,
        List<FieldViolation> fieldErrors
) {
    public record FieldViolation(String field, String message) {}

    public static ApiError of(int status, String code, String message, String path) {
        return new ApiError(Instant.now(), status, code, message, path, null);
    }

    public static ApiError withFieldErrors(int status, String code, String message, String path,
                                           List<FieldViolation> fieldErrors) {
        return new ApiError(Instant.now(), status, code, message, path, fieldErrors);
    }
}
