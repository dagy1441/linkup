package com.siide.linkup.feature.profile.domain.exception;

import com.siide.linkup.core.exception.ResourceNotFoundException;

import java.util.UUID;

public class ProfileNotFoundException extends ResourceNotFoundException {

    public static final String ERROR_CODE = "PROFILE_NOT_FOUND";

    public ProfileNotFoundException(UUID userId) {
        super(ERROR_CODE, "No profile for user " + userId);
    }
}
