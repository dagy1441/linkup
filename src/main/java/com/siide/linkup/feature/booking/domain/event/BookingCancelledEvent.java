package com.siide.linkup.feature.booking.domain.event;

import com.siide.linkup.shared.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record BookingCancelledEvent(
        UUID aggregateId,
        UUID userId,
        UUID activityId,
        int seats,
        Instant occurredAt
) implements DomainEvent {

    public static BookingCancelledEvent of(UUID bookingId, UUID userId, UUID activityId, int seats) {
        return new BookingCancelledEvent(bookingId, userId, activityId, seats, Instant.now());
    }
}
