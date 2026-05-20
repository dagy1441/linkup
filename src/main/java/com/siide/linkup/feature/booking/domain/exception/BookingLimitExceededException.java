package com.siide.linkup.feature.booking.domain.exception;

import com.siide.linkup.core.exception.BusinessRuleViolationException;

public class BookingLimitExceededException extends BusinessRuleViolationException {

    public static final String ERROR_CODE = "BOOKING_LIMIT_EXCEEDED";

    public BookingLimitExceededException(int limit) {
        super(ERROR_CODE, "User has reached the maximum of " + limit + " confirmed bookings");
    }
}
