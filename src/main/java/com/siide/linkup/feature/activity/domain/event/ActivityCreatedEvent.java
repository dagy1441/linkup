package com.siide.linkup.feature.activity.domain.event;

import com.siide.linkup.shared.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record ActivityCreatedEvent(
        UUID aggregateId,
        UUID organizerId,
        String title,
        Instant startsAt,
        int capacity,
        Instant occurredAt
) implements DomainEvent {

    public static ActivityCreatedEvent of(UUID activityId, UUID organizerId,
                                          String title, Instant startsAt, int capacity) {
        return new ActivityCreatedEvent(activityId, organizerId, title, startsAt, capacity, Instant.now());
    }
}
