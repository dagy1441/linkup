package com.siide.linkup.feature.profile.domain.exception;

import com.siide.linkup.core.exception.BusinessRuleViolationException;

/**
 * Raised when the caller uploads a photo that violates the type / size policy
 * (max bytes, content-type allowlist, empty payload). Maps to HTTP 422 via
 * {@link com.siide.linkup.core.exception.GlobalExceptionHandler}.
 */
public class InvalidPhotoException extends BusinessRuleViolationException {

    public static final String ERROR_CODE = "INVALID_PHOTO";

    public InvalidPhotoException(String reason) {
        super(ERROR_CODE, reason);
    }
}
