package com.siide.linkup.feature.booking.application;

import com.siide.linkup.feature.booking.domain.BookingRepository;
import com.siide.linkup.feature.booking.domain.exception.BookingAccessDeniedException;
import com.siide.linkup.feature.booking.domain.exception.BookingNotFoundException;
import com.siide.linkup.feature.booking.domain.model.Booking;
import com.siide.linkup.feature.booking.domain.model.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class BookingQueryService {

    private final BookingRepository repository;
    private final BookingProperties properties;

    public BookingQueryService(BookingRepository repository, BookingProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    public Booking getOwnedById(UUID bookingId, UUID currentUserId) {
        Booking booking = repository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));
        if (!booking.isOwnedBy(currentUserId)) {
            throw new BookingAccessDeniedException(bookingId, currentUserId);
        }
        return booking;
    }

    public Page<Booking> listMine(UUID userId, Optional<BookingStatus> status, Integer page, Integer size) {
        Pageable pageable = resolvePageable(page, size);
        return status
                .map(s -> repository.findByUserIdAndStatus(userId, s, pageable))
                .orElseGet(() -> repository.findByUserId(userId, pageable));
    }

    private Pageable resolvePageable(Integer page, Integer size) {
        int safePage = page == null || page < 0 ? 0 : page;
        int safeSize = size == null || size <= 0 ? properties.defaultPageSize() : size;
        safeSize = Math.min(safeSize, properties.maxPageSize());
        return PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
