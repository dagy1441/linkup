package com.siide.linkup.core.idempotency.exception;

import com.siide.linkup.core.exception.BusinessRuleViolationException;

/**
 * Mapped to HTTP 422. The {@code Idempotency-Key} header is missing, blank, or too long.
 */
public class IdempotencyKeyInvalidException extends BusinessRuleViolationException {

    public static final String ERROR_CODE = "IDEMPOTENCY_KEY_INVALID";

    public IdempotencyKeyInvalidException(String reason) {
        super(ERROR_CODE, "Idempotency-Key is invalid: " + reason);
    }
}
