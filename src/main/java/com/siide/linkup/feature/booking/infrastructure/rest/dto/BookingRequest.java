package com.siide.linkup.feature.booking.infrastructure.rest.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record BookingRequest(
        @NotNull UUID activityId,
        @Min(1) @Max(50) int seats
) {
}
