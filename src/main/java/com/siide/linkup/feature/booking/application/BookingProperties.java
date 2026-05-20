package com.siide.linkup.feature.booking.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Booking-module configuration bound from {@code linkup.booking.*}.
 *
 * @param maxItemsPerUser hard cap on the number of CONFIRMED bookings a single user
 *                        may hold simultaneously (across all activities). Prevents
 *                        abuse / hoarding.
 * @param defaultPageSize default page size for {@code GET /bookings/me}
 * @param maxPageSize     hard upper bound on page size
 */
@ConfigurationProperties(prefix = "linkup.booking")
public record BookingProperties(int maxItemsPerUser, int defaultPageSize, int maxPageSize) {

    public BookingProperties {
        if (maxItemsPerUser <= 0) maxItemsPerUser = 5;
        if (defaultPageSize <= 0) defaultPageSize = 20;
        if (maxPageSize <= 0) maxPageSize = 100;
        if (defaultPageSize > maxPageSize) {
            throw new IllegalArgumentException(
                    "linkup.booking.default-page-size (" + defaultPageSize +
                            ") cannot exceed max-page-size (" + maxPageSize + ")");
        }
    }
}
