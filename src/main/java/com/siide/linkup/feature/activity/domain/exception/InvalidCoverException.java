package com.siide.linkup.feature.activity.domain.exception;

import com.siide.linkup.core.exception.BusinessRuleViolationException;

/**
 * Raised when the organizer uploads a cover that violates the type / size
 * policy (max bytes, content-type allowlist, empty payload). Maps to HTTP 422
 * via {@code GlobalExceptionHandler}.
 */
public class InvalidCoverException extends BusinessRuleViolationException {

    public static final String ERROR_CODE = "INVALID_COVER";

    public InvalidCoverException(String reason) {
        super(ERROR_CODE, reason);
    }
}
