package com.siide.linkup.feature.booking.infrastructure.rest.dto;

import com.siide.linkup.feature.booking.domain.model.Booking;
import com.siide.linkup.feature.booking.domain.model.BookingStatus;

import java.time.Instant;
import java.util.UUID;

public record BookingResponse(
        UUID id,
        UUID activityId,
        int seats,
        BookingStatus status,
        Instant createdAt,
        Instant cancelledAt
) {
    public static BookingResponse from(Booking b) {
        return new BookingResponse(
                b.getId(),
                b.getActivityId(),
                b.getSeats(),
                b.getStatus(),
                b.getCreatedAt(),
                b.getCancelledAt()
        );
    }
}
