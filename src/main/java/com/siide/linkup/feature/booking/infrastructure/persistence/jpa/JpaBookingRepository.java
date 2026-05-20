package com.siide.linkup.feature.booking.infrastructure.persistence.jpa;

import com.siide.linkup.feature.booking.domain.BookingRepository;
import com.siide.linkup.feature.booking.domain.model.Booking;
import com.siide.linkup.feature.booking.domain.model.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface JpaBookingRepository extends JpaRepository<Booking, UUID>, BookingRepository {

    @Override
    Page<Booking> findByUserId(UUID userId, Pageable pageable);

    @Override
    Page<Booking> findByUserIdAndStatus(UUID userId, BookingStatus status, Pageable pageable);

    @Override
    long countByUserIdAndStatus(UUID userId, BookingStatus status);
}
