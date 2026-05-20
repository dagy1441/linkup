package com.siide.linkup.feature.booking.application.dto;

import java.util.UUID;

public record CreateBookingCommand(UUID activityId, int seats) {
}
