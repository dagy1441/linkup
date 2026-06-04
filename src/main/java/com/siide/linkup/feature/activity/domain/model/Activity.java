package com.siide.linkup.feature.activity.domain.model;

import com.siide.linkup.core.audit.Auditable;
import com.siide.linkup.feature.activity.domain.exception.ActivityFullException;
import com.siide.linkup.feature.activity.domain.exception.ActivityInvalidStateException;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
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
 * Activity aggregate root. Owns its capacity bookkeeping ({@code bookedCount}) and
 * lifecycle ({@link ActivityStatus}). Booking module never touches this aggregate
 * directly — it goes through {@code ActivitySeatService}.
 */
@Entity
@Table(
        name = "activities",
        indexes = {
                @Index(name = "ix_activities_status_starts_at", columnList = "status, starts_at"),
                @Index(name = "ix_activities_organizer_id", columnList = "organizer_id")
                // ix_activities_city_lower is a functional index (LOWER(city)) — JPA can't
                // express it; lives in Flyway V6 only.
        }
)
public class Activity extends Auditable {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "description", length = 4000)
    private String description;

    @Embedded
    private Location location;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "capacity", nullable = false)
    private int capacity;

    @Column(name = "booked_count", nullable = false)
    private int bookedCount;

    @Column(name = "organizer_id", nullable = false)
    private UUID organizerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ActivityStatus status;

    /** Object key in the activity-covers bucket (MinIO / S3). Resolved to a URL by the controller. */
    @Column(name = "cover_key", length = 255)
    private String coverKey;

    protected Activity() {
        // JPA
    }

    private Activity(UUID id, String title, String description, Location location,
                     Instant startsAt, int capacity, UUID organizerId, Instant now) {
        this.id = Objects.requireNonNull(id, "id");
        setTitle(title);
        setDescription(description);
        this.location = Objects.requireNonNull(location, "location");
        setStartsAt(startsAt, now);
        setCapacity(capacity, 0);
        this.bookedCount = 0;
        this.organizerId = Objects.requireNonNull(organizerId, "organizerId");
        this.status = ActivityStatus.PUBLISHED;
    }

    public static Activity create(String title, String description, Location location,
                                  Instant startsAt, int capacity, UUID organizerId, Instant now) {
        return new Activity(UUID.randomUUID(), title, description, location,
                startsAt, capacity, organizerId, now);
    }

    /**
     * Update mutable, organizer-controlled fields. Status and {@code bookedCount}
     * are not editable through this path. {@code now} is supplied by the caller to
     * keep the aggregate clock-independent (testability).
     */
    public void update(String title, String description, Location location,
                       Instant startsAt, int capacity, Instant now) {
        requireNotCancelled();
        setTitle(title);
        setDescription(description);
        this.location = Objects.requireNonNull(location, "location");
        setStartsAt(startsAt, now);
        setCapacity(capacity, this.bookedCount);
    }

    public void cancel() {
        if (status == ActivityStatus.CANCELLED) {
            throw new ActivityInvalidStateException("Activity is already cancelled");
        }
        this.status = ActivityStatus.CANCELLED;
    }

    public boolean isOrganizedBy(UUID userId) {
        return organizerId.equals(userId);
    }

    public boolean isOpenForBooking(Instant now) {
        return status == ActivityStatus.PUBLISHED
                && startsAt.isAfter(now)
                && bookedCount < capacity;
    }

    /** Domain operation: book {@code qty} seats. Used by {@code ActivitySeatService}. */
    public void reserveSeats(int qty, Instant now) {
        if (qty <= 0) {
            throw new IllegalArgumentException("qty must be > 0");
        }
        if (status != ActivityStatus.PUBLISHED) {
            throw new ActivityInvalidStateException("Cannot book a non-published activity");
        }
        if (!startsAt.isAfter(now)) {
            throw new ActivityInvalidStateException("Cannot book an activity that has already started");
        }
        if (bookedCount + qty > capacity) {
            throw new ActivityFullException(id);
        }
        this.bookedCount += qty;
    }

    public void releaseSeats(int qty) {
        if (qty <= 0) {
            throw new IllegalArgumentException("qty must be > 0");
        }
        this.bookedCount = Math.max(0, this.bookedCount - qty);
    }

    /** Attach the storage key returned by the cover upload. Refused on CANCELLED activities. */
    public void attachCover(String coverKey) {
        requireNotCancelled();
        this.coverKey = coverKey;
    }

    /** Detach the current cover. Caller is responsible for deleting the object from storage. */
    public void clearCover() {
        requireNotCancelled();
        this.coverKey = null;
    }

    public String getCoverKey() {
        return coverKey;
    }

    private void setTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        if (title.length() > 150) {
            throw new IllegalArgumentException("title must be at most 150 characters");
        }
        this.title = title.trim();
    }

    private void setDescription(String description) {
        if (description != null && description.length() > 4000) {
            throw new IllegalArgumentException("description must be at most 4000 characters");
        }
        this.description = description == null || description.isBlank() ? null : description.trim();
    }

    private void setStartsAt(Instant startsAt, Instant now) {
        Objects.requireNonNull(startsAt, "startsAt");
        Objects.requireNonNull(now, "now");
        if (!startsAt.isAfter(now)) {
            throw new IllegalArgumentException("startsAt must be in the future");
        }
        this.startsAt = startsAt;
    }

    private void setCapacity(int capacity, int minimum) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be > 0");
        }
        if (capacity < minimum) {
            throw new ActivityInvalidStateException(
                    "capacity (" + capacity + ") cannot be lower than already booked seats (" + minimum + ")");
        }
        this.capacity = capacity;
    }

    private void requireNotCancelled() {
        if (status == ActivityStatus.CANCELLED) {
            throw new ActivityInvalidStateException("Cannot modify a cancelled activity");
        }
    }

    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Location getLocation() { return location; }
    public Instant getStartsAt() { return startsAt; }
    public int getCapacity() { return capacity; }
    public int getBookedCount() { return bookedCount; }
    public int getRemainingSeats() { return capacity - bookedCount; }
    public UUID getOrganizerId() { return organizerId; }
    public ActivityStatus getStatus() { return status; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Activity other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
