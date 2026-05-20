package com.siide.linkup.core.exception;

/**
 * Base class for all domain-level exceptions raised by feature modules.
 * <p>
 * Concrete subclasses must declare a stable {@code errorCode} that maps to a
 * specific HTTP status via {@link GlobalExceptionHandler}.
 */
public abstract class DomainException extends RuntimeException {

    private final String errorCode;

    protected DomainException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    protected DomainException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
