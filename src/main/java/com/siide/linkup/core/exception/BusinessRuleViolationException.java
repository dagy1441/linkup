package com.siide.linkup.core.exception;

/**
 * Thrown when an invariant or business rule is violated. Mapped to HTTP 422.
 */
public class BusinessRuleViolationException extends DomainException {

    public BusinessRuleViolationException(String errorCode, String message) {
        super(errorCode, message);
    }
}
