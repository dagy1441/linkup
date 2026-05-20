package com.siide.linkup.feature.activity.domain.event;

import com.siide.linkup.shared.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record ActivityCancelledEvent(
        UUID aggregateId,
        UUID organizerId,
        Instant occurredAt
) implements DomainEvent {

    public static ActivityCancelledEvent of(UUID activityId, UUID organizerId) {
        return new ActivityCancelledEvent(activityId, organizerId, Instant.now());
    }
}
