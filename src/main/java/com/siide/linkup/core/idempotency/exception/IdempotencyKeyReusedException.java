package com.siide.linkup.core.idempotency.exception;

import com.siide.linkup.core.exception.BusinessRuleViolationException;

/**
 * Mapped to HTTP 422. The same {@code Idempotency-Key} was previously used with a
 * different request body — the client is using the key incorrectly.
 */
public class IdempotencyKeyReusedException extends BusinessRuleViolationException {

    public static final String ERROR_CODE = "IDEMPOTENCY_KEY_REUSED";

    public IdempotencyKeyReusedException(String key) {
        super(ERROR_CODE, "Idempotency-Key '" + key + "' was already used with a different request body");
    }
}
