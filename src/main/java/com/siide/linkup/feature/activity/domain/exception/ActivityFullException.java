package com.siide.linkup.feature.activity.domain.exception;

import com.siide.linkup.core.exception.ConflictException;

import java.util.UUID;

public class ActivityFullException extends ConflictException {

    public static final String ERROR_CODE = "ACTIVITY_FULL";

    public ActivityFullException(UUID activityId) {
        super(ERROR_CODE, "Activity " + activityId + " has reached its capacity");
    }
}
