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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookingCommandServiceTest {

    private BookingRepository repository;
    private ActivitySeatService seatService;
    private ApplicationEventPublisher publisher;
    private BookingCommandService service;

    private final Instant now = Instant.parse("2026-01-01T10:00:00Z");
    private final UUID userId = UUID.randomUUID();
    private final UUID activityId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        repository = mock(BookingRepository.class);
        seatService = mock(ActivitySeatService.class);
        publisher = mock(ApplicationEventPublisher.class);
        BookingProperties props = new BookingProperties(5, 20, 100);
        service = new BookingCommandService(repository, seatService, publisher, props,
                Clock.fixed(now, ZoneOffset.UTC));
    }

    @Test
    void create_reserves_seats_persists_booking_and_publishes_event() {
        when(repository.countByUserIdAndStatus(userId, BookingStatus.CONFIRMED)).thenReturn(0L);
        when(seatService.tryReserveSeats(activityId, 2)).thenReturn(true);
        when(repository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        Booking created = service.create(new CreateBookingCommand(activityId, 2), userId);

        assertThat(created.getUserId()).isEqualTo(userId);
        assertThat(created.getSeats()).isEqualTo(2);
        assertThat(created.isConfirmed()).isTrue();
        verify(publisher).publishEvent(any(BookingCreatedEvent.class));
    }

    @Test
    void create_throws_when_activity_cannot_accept_booking() {
        when(repository.countByUserIdAndStatus(userId, BookingStatus.CONFIRMED)).thenReturn(0L);
        when(seatService.tryReserveSeats(activityId, 1)).thenReturn(false);

        assertThatThrownBy(() -> service.create(new CreateBookingCommand(activityId, 1), userId))
                .isInstanceOf(ActivityNotBookableException.class);

        verify(repository, never()).save(any());
        verify(publisher, never()).publishEvent(any());
    }

    @Test
    void create_throws_when_user_at_max_items_cap() {
        when(repository.countByUserIdAndStatus(userId, BookingStatus.CONFIRMED)).thenReturn(5L);

        assertThatThrownBy(() -> service.create(new CreateBookingCommand(activityId, 1), userId))
                .isInstanceOf(BookingLimitExceededException.class);

        verify(seatService, never()).tryReserveSeats(any(), anyInt());
    }

    @Test
    void create_throws_when_seats_is_zero() {
        assertThatThrownBy(() -> service.create(new CreateBookingCommand(activityId, 0), userId))
                .isInstanceOf(BookingInvalidStateException.class);
    }

    @Test
    void create_translates_unique_violation_to_invalid_state() {
        when(repository.countByUserIdAndStatus(userId, BookingStatus.CONFIRMED)).thenReturn(0L);
        when(seatService.tryReserveSeats(activityId, 1)).thenReturn(true);
        when(repository.save(any(Booking.class))).thenThrow(new DataIntegrityViolationException("uk"));

        assertThatThrownBy(() -> service.create(new CreateBookingCommand(activityId, 1), userId))
                .isInstanceOf(BookingInvalidStateException.class);
    }

    @Test
    void cancel_transitions_aggregate_releases_seats_and_publishes_event() {
        Booking existing = Booking.confirm(userId, activityId, 3);
        when(repository.findById(existing.getId())).thenReturn(Optional.of(existing));

        Booking cancelled = service.cancel(existing.getId(), userId);

        assertThat(cancelled.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        verify(seatService).releaseSeats(activityId, 3);
        verify(publisher).publishEvent(any(BookingCancelledEvent.class));
    }

    @Test
    void cancel_throws_when_booking_not_found() {
        UUID bookingId = UUID.randomUUID();
        when(repository.findById(bookingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancel(bookingId, userId))
                .isInstanceOf(BookingNotFoundException.class);
    }

    @Test
    void cancel_throws_when_user_does_not_own_booking() {
        Booking existing = Booking.confirm(userId, activityId, 1);
        when(repository.findById(existing.getId())).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.cancel(existing.getId(), UUID.randomUUID()))
                .isInstanceOf(BookingAccessDeniedException.class);

        verify(seatService, never()).releaseSeats(eq(activityId), anyInt());
    }
}
