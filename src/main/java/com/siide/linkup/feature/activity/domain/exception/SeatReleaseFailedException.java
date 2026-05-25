package com.siide.linkup.feature.activity.domain.exception;

import com.siide.linkup.core.exception.ConflictException;

import java.util.UUID;

/**
 * Raised when a seat-release operation cannot be applied because the activity's
 * {@code booked_count} is lower than the quantity asked to release (data drift,
 * double-cancel attempt, or unknown activity id). The caller's transaction must
 * roll back so the booking is not marked cancelled while seats stay stuck.
 */
public class SeatReleaseFailedException extends ConflictException {

    public static final String ERROR_CODE = "SEAT_RELEASE_FAILED";

    public SeatReleaseFailedException(UUID activityId, int qty) {
        super(ERROR_CODE,
                "Cannot release " + qty + " seat(s) on activity " + activityId
                        + " — booked count is lower than requested or activity is missing");
    }
}
