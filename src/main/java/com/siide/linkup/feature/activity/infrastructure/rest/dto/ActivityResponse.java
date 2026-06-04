package com.siide.linkup.feature.activity.infrastructure.rest.dto;

import com.siide.linkup.feature.activity.domain.model.Activity;
import com.siide.linkup.feature.activity.domain.model.ActivityStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Public-facing projection of an {@link Activity}. The internal {@code organizerId} is
 * never exposed; consumers see the organizer's display name instead.
 */
public record ActivityResponse(
        UUID id,
        String title,
        String description,
        String city,
        String addressLine,
        Double latitude,
        Double longitude,
        Instant startsAt,
        int capacity,
        int bookedCount,
        int remainingSeats,
        String organizerDisplayName,
        ActivityStatus status,
        /** Internal storage key — kept for ops / migration, not for UI consumption. */
        String coverKey,
        /** Time-limited presigned URL the browser can {@code <img src>} directly. */
        String coverUrl
) {
    public static ActivityResponse from(Activity a, String organizerDisplayName, String coverUrl) {
        return new ActivityResponse(
                a.getId(),
                a.getTitle(),
                a.getDescription(),
                a.getLocation().getCity(),
                a.getLocation().getAddressLine(),
                a.getLocation().getLatitude(),
                a.getLocation().getLongitude(),
                a.getStartsAt(),
                a.getCapacity(),
                a.getBookedCount(),
                a.getRemainingSeats(),
                organizerDisplayName,
                a.getStatus(),
                a.getCoverKey(),
                coverUrl
        );
    }

    /** Convenience overload for callers that don't have a presigned URL on hand (tests, list views without enrichment). */
    public static ActivityResponse from(Activity a, String organizerDisplayName) {
        return from(a, organizerDisplayName, null);
    }
}
