package com.siide.linkup.shared.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Marker interface for inter-module domain events.
 * Implementations are immutable records published via Spring's {@code ApplicationEventPublisher}.
 */
public interface DomainEvent {

    /** Identifier of the aggregate that produced the event. */
    UUID aggregateId();

    /** Instant the event was produced (UTC). */
    Instant occurredAt();
}
