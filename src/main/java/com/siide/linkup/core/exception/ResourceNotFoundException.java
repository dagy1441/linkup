package com.siide.linkup.core.exception;

/**
 * Thrown when a referenced aggregate cannot be located. Mapped to HTTP 404.
 */
public class ResourceNotFoundException extends DomainException {

    public ResourceNotFoundException(String errorCode, String message) {
        super(errorCode, message);
    }
}
