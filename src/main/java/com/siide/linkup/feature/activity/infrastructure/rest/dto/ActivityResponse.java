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
        ActivityStatus status
) {
    public static ActivityResponse from(Activity a, String organizerDisplayName) {
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
                a.getStatus()
        );
    }
}
