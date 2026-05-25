package com.siide.linkup.feature.profile.domain.event;

import com.siide.linkup.shared.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Published the first time a user's profile becomes "complete" (bio + city + DOB
 * filled). Downstream consumers: {@code recommendation} (V2) to seed the feed,
 * {@code notification} (S2) to send a "welcome to the community" email.
 */
public record ProfileCompletedEvent(
        UUID profileId,
        UUID userId,
        Instant occurredAt
) implements DomainEvent {

    public static ProfileCompletedEvent of(UUID profileId, UUID userId, Instant occurredAt) {
        return new ProfileCompletedEvent(profileId, userId, occurredAt);
    }

    @Override
    public UUID aggregateId() {
        return profileId;
    }
}
