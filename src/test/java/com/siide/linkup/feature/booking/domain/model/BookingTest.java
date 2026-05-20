package com.siide.linkup.feature.booking.domain.model;

import com.siide.linkup.feature.booking.domain.exception.BookingInvalidStateException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BookingTest {

    private final UUID userId = UUID.randomUUID();
    private final UUID activityId = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-01-01T10:00:00Z");

    @Test
    void confirm_creates_a_booking_in_confirmed_state() {
        Booking b = Booking.confirm(userId, activityId, 2);

        assertThat(b.getId()).isNotNull();
        assertThat(b.getUserId()).isEqualTo(userId);
        assertThat(b.getActivityId()).isEqualTo(activityId);
        assertThat(b.getSeats()).isEqualTo(2);
        assertThat(b.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(b.getCancelledAt()).isNull();
        assertThat(b.isConfirmed()).isTrue();
    }

    @Test
    void confirm_rejects_non_positive_seats() {
        assertThatThrownBy(() -> Booking.confirm(userId, activityId, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("seats");
    }

    @Test
    void cancel_transitions_to_cancelled_and_stamps_timestamp() {
        Booking b = Booking.confirm(userId, activityId, 1);
        b.cancel(now);

        assertThat(b.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(b.getCancelledAt()).isEqualTo(now);
        assertThat(b.isConfirmed()).isFalse();
    }

    @Test
    void cancel_twice_is_rejected() {
        Booking b = Booking.confirm(userId, activityId, 1);
        b.cancel(now);

        assertThatThrownBy(() -> b.cancel(now))
                .isInstanceOf(BookingInvalidStateException.class);
    }

    @Test
    void is_owned_by_returns_true_only_for_creator() {
        Booking b = Booking.confirm(userId, activityId, 1);

        assertThat(b.isOwnedBy(userId)).isTrue();
        assertThat(b.isOwnedBy(UUID.randomUUID())).isFalse();
    }
}
