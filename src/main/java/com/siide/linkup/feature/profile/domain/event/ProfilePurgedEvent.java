package com.siide.linkup.feature.profile.domain.event;

import com.siide.linkup.shared.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Published right BEFORE the profile row is hard-deleted by the scheduler.
 * Consumers — {@code feature.auth} (disable Keycloak user), {@code booking}
 * (anonymise past bookings), {@code notification} (final "your data is gone"
 * email) — must finish their work in this transaction.
 */
public record ProfilePurgedEvent(
        UUID profileId,
        UUID userId,
        Instant occurredAt
) implements DomainEvent {

    public static ProfilePurgedEvent of(UUID profileId, UUID userId, Instant occurredAt) {
        return new ProfilePurgedEvent(profileId, userId, occurredAt);
    }

    @Override
    public UUID aggregateId() {
        return profileId;
    }
}
