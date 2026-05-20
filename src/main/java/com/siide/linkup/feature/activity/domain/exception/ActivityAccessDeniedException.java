package com.siide.linkup.feature.activity.domain.exception;

import com.siide.linkup.core.exception.ForbiddenOperationException;

import java.util.UUID;

public class ActivityAccessDeniedException extends ForbiddenOperationException {

    public static final String ERROR_CODE = "ACTIVITY_ACCESS_DENIED";

    public ActivityAccessDeniedException(UUID activityId, UUID userId) {
        super(ERROR_CODE, "User " + userId + " is not the organizer of activity " + activityId);
    }
}
