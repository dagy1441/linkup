package com.siide.linkup.feature.activity.domain.exception;

import com.siide.linkup.core.exception.BusinessRuleViolationException;

public class ActivityInvalidStateException extends BusinessRuleViolationException {

    public static final String ERROR_CODE = "ACTIVITY_INVALID_STATE";

    public ActivityInvalidStateException(String message) {
        super(ERROR_CODE, message);
    }
}
