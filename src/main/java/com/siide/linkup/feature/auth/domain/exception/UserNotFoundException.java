package com.siide.linkup.feature.auth.domain.exception;

import com.siide.linkup.core.exception.ResourceNotFoundException;

public class UserNotFoundException extends ResourceNotFoundException {

    public static final String ERROR_CODE = "USER_NOT_FOUND";

    public UserNotFoundException(String message) {
        super(ERROR_CODE, message);
    }

    public static UserNotFoundException byKeycloakId(String keycloakId) {
        return new UserNotFoundException("No local user provisioned for keycloakId=" + keycloakId);
    }
}
