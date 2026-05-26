package com.siide.linkup.feature.profile.domain.event;

import com.siide.linkup.shared.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Published when a user requests account deletion. Downstream consumers:
 * {@code notification} (S2) to send a "we'll delete your account on X" email,
 * potentially {@code booking} to start refunding upcoming bookings.
 * <p>
 * {@link #scheduledPurgeAt} is the precise instant after which the daily
 * scheduler will hard-purge the profile. The user can cancel via
 * {@code POST /api/v1/profile/me/restore} before then.
 */
public record ProfileDeletionRequestedEvent(
        UUID profileId,
        UUID userId,
        Instant scheduledPurgeAt,
        Instant occurredAt
) implements DomainEvent {

    public static ProfileDeletionRequestedEvent of(UUID profileId, UUID userId,
                                                    Instant scheduledPurgeAt, Instant occurredAt) {
        return new ProfileDeletionRequestedEvent(profileId, userId, scheduledPurgeAt, occurredAt);
    }

    @Override
    public UUID aggregateId() {
        return profileId;
    }
}
