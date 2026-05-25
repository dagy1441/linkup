package com.siide.linkup.feature.profile.application.dto;

import com.siide.linkup.feature.profile.domain.model.Gender;

import java.time.LocalDate;

/**
 * Carries the user-editable profile fields. {@code null} fields follow the
 * semantics defined on {@link com.siide.linkup.feature.profile.domain.model.Profile#update}.
 */
public record UpdateProfileCommand(
        String bio,
        String city,
        LocalDate dateOfBirth,
        Gender gender
) {}
