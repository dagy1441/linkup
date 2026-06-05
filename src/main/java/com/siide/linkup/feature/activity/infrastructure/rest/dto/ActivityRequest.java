package com.siide.linkup.feature.activity.infrastructure.rest.dto;

import com.siide.linkup.feature.activity.domain.model.ActivityCategory;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record ActivityRequest(
        @NotBlank @Size(max = 150) String title,
        @Size(max = 4000) String description,
        @NotNull ActivityCategory category,
        @NotBlank @Size(max = 100) String city,
        @Size(max = 250) String addressLine,
        Double latitude,
        Double longitude,
        @NotNull @Future Instant startsAt,
        @Positive int capacity
) {}
