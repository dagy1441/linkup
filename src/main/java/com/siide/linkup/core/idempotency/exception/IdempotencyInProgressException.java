package com.siide.linkup.core.idempotency.exception;

import com.siide.linkup.core.exception.ConflictException;

/**
 * Mapped to HTTP 409. An identical request with the same {@code Idempotency-Key} is
 * still being processed. The client should retry after a short delay.
 */
public class IdempotencyInProgressException extends ConflictException {

    public static final String ERROR_CODE = "IDEMPOTENCY_IN_PROGRESS";

    public IdempotencyInProgressException(String key) {
        super(ERROR_CODE, "Idempotency-Key '" + key + "' is still being processed; retry shortly");
    }
}
