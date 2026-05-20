package com.siide.linkup.feature.booking.domain.exception;

import com.siide.linkup.core.exception.BusinessRuleViolationException;

public class BookingInvalidStateException extends BusinessRuleViolationException {

    public static final String ERROR_CODE = "BOOKING_INVALID_STATE";

    public BookingInvalidStateException(String message) {
        super(ERROR_CODE, message);
    }
}
