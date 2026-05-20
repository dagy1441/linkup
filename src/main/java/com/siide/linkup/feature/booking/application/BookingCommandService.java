package com.siide.linkup.feature.booking.application;

import com.siide.linkup.feature.activity.api.ActivitySeatService;
import com.siide.linkup.feature.booking.application.dto.CreateBookingCommand;
import com.siide.linkup.feature.booking.domain.BookingRepository;
import com.siide.linkup.feature.booking.domain.event.BookingCancelledEvent;
import com.siide.linkup.feature.booking.domain.event.BookingCreatedEvent;
import com.siide.linkup.feature.booking.domain.exception.ActivityNotBookableException;
import com.siide.linkup.feature.booking.domain.exception.BookingAccessDeniedException;
import com.siide.linkup.feature.booking.domain.exception.BookingInvalidStateException;
import com.siide.linkup.feature.booking.domain.exception.BookingLimitExceededException;
import com.siide.linkup.feature.booking.domain.exception.BookingNotFoundException;
import com.siide.linkup.feature.booking.domain.model.Booking;
import com.siide.linkup.feature.booking.domain.model.BookingStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Write-side use-cases for bookings. Coordinates seat reservation against the
 * activity module and persists the booking aggregate. The transaction wraps both
 * sides so a failed insert (e.g. unique-constraint violation on re-booking) releases
 * the seats automatically via rollback of the seat increment.
 */
@Service
public class BookingCommandService {

    private static final Logger log = LoggerFactory.getLogger(BookingCommandService.class);

    private final BookingRepository repository;
    private final ActivitySeatService seatService;
    private final ApplicationEventPublisher eventPublisher;
    private final BookingProperties properties;
    private final Clock clock;

    public BookingCommandService(BookingRepository repository,
                                 ActivitySeatService seatService,
                                 ApplicationEventPublisher eventPublisher,
                                 BookingProperties properties,
                                 Clock clock) {
        this.repository = repository;
        this.seatService = seatService;
        this.eventPublisher = eventPublisher;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public Booking create(CreateBookingCommand cmd, UUID userId) {
        if (cmd.seats() <= 0) {
            throw new BookingInvalidStateException("seats must be > 0");
        }

        long confirmed = repository.countByUserIdAndStatus(userId, BookingStatus.CONFIRMED);
        if (confirmed >= properties.maxItemsPerUser()) {
            throw new BookingLimitExceededException(properties.maxItemsPerUser());
        }

        if (!seatService.tryReserveSeats(cmd.activityId(), cmd.seats())) {
            throw new ActivityNotBookableException(cmd.activityId(), cmd.seats());
        }

        Booking booking = Booking.confirm(userId, cmd.activityId(), cmd.seats());
        Booking saved;
        try {
            saved = repository.save(booking);
        } catch (DataIntegrityViolationException e) {
            // Partial unique index (user, activity) WHERE CONFIRMED — re-booking attempt.
            // The transaction will roll back, which also reverts the seat reservation.
            log.info("Duplicate booking attempt user={} activity={}", userId, cmd.activityId());
            throw new BookingInvalidStateException(
                    "User already has a confirmed booking for activity " + cmd.activityId());
        }

        eventPublisher.publishEvent(BookingCreatedEvent.of(
                saved.getId(), saved.getUserId(), saved.getActivityId(), saved.getSeats()));
        log.info("Booking created id={} userId={} activityId={} seats={}",
                saved.getId(), userId, saved.getActivityId(), saved.getSeats());
        return saved;
    }

    @Transactional
    public Booking cancel(UUID bookingId, UUID currentUserId) {
        Booking booking = repository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));
        if (!booking.isOwnedBy(currentUserId)) {
            throw new BookingAccessDeniedException(bookingId, currentUserId);
        }

        booking.cancel(Instant.now(clock));
        // Release seats *after* the aggregate transition: if the release fails we don't
        // want a cancelled-but-still-seated booking. Both calls share the same transaction.
        seatService.releaseSeats(booking.getActivityId(), booking.getSeats());

        eventPublisher.publishEvent(BookingCancelledEvent.of(
                booking.getId(), booking.getUserId(), booking.getActivityId(), booking.getSeats()));
        log.info("Booking cancelled id={} userId={} activityId={} seats={}",
                booking.getId(), currentUserId, booking.getActivityId(), booking.getSeats());
        return booking;
    }
}
