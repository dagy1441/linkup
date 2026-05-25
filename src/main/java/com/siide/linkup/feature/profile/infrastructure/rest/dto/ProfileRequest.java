package com.siide.linkup.feature.profile.infrastructure.rest.dto;

import com.siide.linkup.feature.profile.domain.model.Gender;
import com.siide.linkup.feature.profile.domain.model.Profile;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * PUT /profile/me request body. All fields optional — the controller treats
 * {@code null} per the domain semantics on {@link Profile#update}.
 */
public record ProfileRequest(
        @Size(max = Profile.BIO_MAX_LENGTH) String bio,
        @Size(max = Profile.CITY_MAX_LENGTH) String city,
        @Past LocalDate dateOfBirth,
        Gender gender
) {}
