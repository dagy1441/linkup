package com.siide.linkup.feature.booking.domain.exception;

import com.siide.linkup.core.exception.ConflictException;

import java.util.UUID;

/**
 * Thrown when seat reservation against the activity module fails: the activity is
 * cancelled, has already started, doesn't exist, or has insufficient remaining capacity.
 * The booking module does not distinguish reasons further — that detail belongs to
 * the activity module's own exceptions, which are not part of this aggregate's
 * concerns.
 */
public class ActivityNotBookableException extends ConflictException {

    public static final String ERROR_CODE = "ACTIVITY_NOT_BOOKABLE";

    public ActivityNotBookableException(UUID activityId, int seats) {
        super(ERROR_CODE, "Activity " + activityId + " cannot accept a booking of " + seats + " seat(s)");
    }
}
