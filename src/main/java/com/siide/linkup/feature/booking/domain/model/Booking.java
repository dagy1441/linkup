package com.siide.linkup.feature.booking.domain.model;

import com.siide.linkup.core.audit.Auditable;
import com.siide.linkup.feature.booking.domain.exception.BookingInvalidStateException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Booking aggregate. Holds the link between a user and an activity for {@code seats}
 * places. Once cancelled, a booking is immutable. Seat capacity bookkeeping lives on
 * the activity side — this aggregate only owns its own lifecycle.
 */
@Entity
@Table(
        name = "bookings",
        indexes = {
                @Index(name = "ix_bookings_user_id", columnList = "user_id"),
                @Index(name = "ix_bookings_activity_id", columnList = "activity_id"),
                @Index(name = "ix_bookings_status", columnList = "status")
        }
)
public class Booking extends Auditable {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "activity_id", nullable = false, updatable = false)
    private UUID activityId;

    @Column(name = "seats", nullable = false, updatable = false)
    private int seats;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BookingStatus status;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    protected Booking() {
        // JPA
    }

    private Booking(UUID id, UUID userId, UUID activityId, int seats) {
        this.id = Objects.requireNonNull(id, "id");
        this.userId = Objects.requireNonNull(userId, "userId");
        this.activityId = Objects.requireNonNull(activityId, "activityId");
        if (seats <= 0) {
            throw new IllegalArgumentException("seats must be > 0");
        }
        this.seats = seats;
        this.status = BookingStatus.CONFIRMED;
    }

    public static Booking confirm(UUID userId, UUID activityId, int seats) {
        return new Booking(UUID.randomUUID(), userId, activityId, seats);
    }

    public void cancel(Instant now) {
        Objects.requireNonNull(now, "now");
        if (status == BookingStatus.CANCELLED) {
            throw new BookingInvalidStateException("Booking is already cancelled");
        }
        this.status = BookingStatus.CANCELLED;
        this.cancelledAt = now;
    }

    public boolean isOwnedBy(UUID candidateUserId) {
        return userId.equals(candidateUserId);
    }

    public boolean isConfirmed() {
        return status == BookingStatus.CONFIRMED;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getActivityId() { return activityId; }
    public int getSeats() { return seats; }
    public BookingStatus getStatus() { return status; }
    public Instant getCancelledAt() { return cancelledAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Booking other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
