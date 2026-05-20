package com.siide.linkup.feature.booking.domain.exception;

import com.siide.linkup.core.exception.ForbiddenOperationException;

import java.util.UUID;

public class BookingAccessDeniedException extends ForbiddenOperationException {

    public static final String ERROR_CODE = "BOOKING_ACCESS_DENIED";

    public BookingAccessDeniedException(UUID bookingId, UUID userId) {
        super(ERROR_CODE, "User " + userId + " is not allowed to operate on booking " + bookingId);
    }
}
