package com.siide.linkup.feature.booking.domain.event;

import com.siide.linkup.shared.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record BookingCreatedEvent(
        UUID aggregateId,
        UUID userId,
        UUID activityId,
        int seats,
        Instant occurredAt
) implements DomainEvent {

    public static BookingCreatedEvent of(UUID bookingId, UUID userId, UUID activityId, int seats) {
        return new BookingCreatedEvent(bookingId, userId, activityId, seats, Instant.now());
    }
}
