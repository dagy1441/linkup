package com.siide.linkup.feature.booking.domain.exception;

import com.siide.linkup.core.exception.ResourceNotFoundException;

import java.util.UUID;

public class BookingNotFoundException extends ResourceNotFoundException {

    public static final String ERROR_CODE = "BOOKING_NOT_FOUND";

    public BookingNotFoundException(UUID bookingId) {
        super(ERROR_CODE, "Booking " + bookingId + " not found");
    }
}
