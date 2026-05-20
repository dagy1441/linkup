package com.siide.linkup.feature.activity.domain.exception;

import com.siide.linkup.core.exception.ResourceNotFoundException;

import java.util.UUID;

public class ActivityNotFoundException extends ResourceNotFoundException {

    public static final String ERROR_CODE = "ACTIVITY_NOT_FOUND";

    public ActivityNotFoundException(UUID id) {
        super(ERROR_CODE, "No activity found with id=" + id);
    }
}
