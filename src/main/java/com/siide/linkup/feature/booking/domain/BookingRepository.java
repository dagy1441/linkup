package com.siide.linkup.feature.booking.domain;

import com.siide.linkup.feature.booking.domain.model.Booking;
import com.siide.linkup.feature.booking.domain.model.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface BookingRepository {

    Booking save(Booking booking);

    Optional<Booking> findById(UUID id);

    Page<Booking> findByUserId(UUID userId, Pageable pageable);

    Page<Booking> findByUserIdAndStatus(UUID userId, BookingStatus status, Pageable pageable);

    long countByUserIdAndStatus(UUID userId, BookingStatus status);
}
