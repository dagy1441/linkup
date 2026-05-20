package com.siide.linkup.core.exception;

/**
 * Thrown when the authenticated user attempts an action they are not allowed to perform
 * on a given resource (e.g. updating an activity they do not own). Mapped to HTTP 403.
 */
public class ForbiddenOperationException extends DomainException {

    public ForbiddenOperationException(String errorCode, String message) {
        super(errorCode, message);
    }
}
