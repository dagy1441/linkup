package com.siide.linkup.feature.auth.domain.event;

import com.siide.linkup.shared.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Emitted the first time a Keycloak identity is materialised as a local {@code users} row.
 * Other modules (notification, recommendation, ...) may subscribe to react to onboarding.
 */
public record UserProvisionedEvent(
        UUID aggregateId,
        String keycloakId,
        String email,
        Instant occurredAt
) implements DomainEvent {

    public static UserProvisionedEvent of(UUID userId, String keycloakId, String email) {
        return new UserProvisionedEvent(userId, keycloakId, email, Instant.now());
    }
}
