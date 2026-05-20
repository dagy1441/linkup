package com.siide.linkup.core.exception;

/**
 * Thrown when an operation conflicts with the current state (duplicate, concurrent edit).
 * Mapped to HTTP 409.
 */
public class ConflictException extends DomainException {

    public ConflictException(String errorCode, String message) {
        super(errorCode, message);
    }
}
