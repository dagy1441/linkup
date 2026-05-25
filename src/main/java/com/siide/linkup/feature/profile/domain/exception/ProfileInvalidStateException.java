package com.siide.linkup.feature.profile.domain.exception;

import com.siide.linkup.core.exception.BusinessRuleViolationException;

public class ProfileInvalidStateException extends BusinessRuleViolationException {

    public static final String ERROR_CODE = "PROFILE_INVALID_STATE";

    public ProfileInvalidStateException(String message) {
        super(ERROR_CODE, message);
    }
}
