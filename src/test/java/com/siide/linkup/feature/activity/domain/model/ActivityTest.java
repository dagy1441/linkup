package com.siide.linkup.feature.activity.domain.model;

import com.siide.linkup.feature.activity.domain.exception.ActivityFullException;
import com.siide.linkup.feature.activity.domain.exception.ActivityInvalidStateException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ActivityTest {

    private final UUID organizerId = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-01-01T10:00:00Z");
    private final Instant inOneHour = now.plus(1, ChronoUnit.HOURS);

    @Test
    void create_initialises_published_activity_with_zero_bookings() {
        Activity a = newActivity(10);

        assertThat(a.getId()).isNotNull();
        assertThat(a.getStatus()).isEqualTo(ActivityStatus.PUBLISHED);
        assertThat(a.getBookedCount()).isZero();
        assertThat(a.getRemainingSeats()).isEqualTo(10);
        assertThat(a.isOrganizedBy(organizerId)).isTrue();
    }

    @Test
    void create_rejects_past_start_date() {
        assertThatThrownBy(() -> Activity.create("T", null, Location.ofCity("Abidjan"),
                now.minus(1, ChronoUnit.MINUTES), 5, organizerId, now))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("startsAt");
    }

    @Test
    void create_rejects_non_positive_capacity() {
        assertThatThrownBy(() -> Activity.create("T", null, Location.ofCity("Abidjan"),
                inOneHour, 0, organizerId, now))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("capacity");
    }

    @Test
    void create_rejects_blank_title() {
        assertThatThrownBy(() -> Activity.create(" ", null, Location.ofCity("Abidjan"),
                inOneHour, 5, organizerId, now))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("title");
    }

    @Test
    void reserve_seat_increments_booked_count() {
        Activity a = newActivity(2);
        a.reserveSeats(1, now);
        a.reserveSeats(1, now);

        assertThat(a.getBookedCount()).isEqualTo(2);
        assertThat(a.getRemainingSeats()).isZero();
    }

    @Test
    void reserve_seat_throws_when_full() {
        Activity a = newActivity(1);
        a.reserveSeats(1, now);

        assertThatThrownBy(() -> a.reserveSeats(1, now))
                .isInstanceOf(ActivityFullException.class);
    }

    @Test
    void reserve_seat_throws_when_cancelled() {
        Activity a = newActivity(1);
        a.cancel();

        assertThatThrownBy(() -> a.reserveSeats(1, now))
                .isInstanceOf(ActivityInvalidStateException.class);
    }

    @Test
    void release_seats_never_goes_below_zero() {
        Activity a = newActivity(2);
        a.releaseSeats(5);
        assertThat(a.getBookedCount()).isZero();
    }

    @Test
    void reserve_multiple_seats_at_once() {
        Activity a = newActivity(5);
        a.reserveSeats(3, now);
        assertThat(a.getBookedCount()).isEqualTo(3);
        assertThat(a.getRemainingSeats()).isEqualTo(2);
    }

    @Test
    void reserve_seats_throws_when_request_exceeds_remaining() {
        Activity a = newActivity(5);
        a.reserveSeats(4, now);
        assertThatThrownBy(() -> a.reserveSeats(2, now))
                .isInstanceOf(ActivityFullException.class);
    }

    @Test
    void cancel_twice_is_rejected() {
        Activity a = newActivity(5);
        a.cancel();
        assertThatThrownBy(a::cancel).isInstanceOf(ActivityInvalidStateException.class);
    }

    @Test
    void update_rejected_when_capacity_below_booked_count() {
        Activity a = newActivity(5);
        a.reserveSeats(1, now);
        a.reserveSeats(1, now);

        assertThatThrownBy(() -> a.update("New", null, Location.ofCity("Abidjan"),
                inOneHour, 1, now))
                .isInstanceOf(ActivityInvalidStateException.class)
                .hasMessageContaining("booked");
    }

    @Test
    void update_rejected_on_cancelled_activity() {
        Activity a = newActivity(5);
        a.cancel();
        assertThatThrownBy(() -> a.update("New", null, Location.ofCity("Abidjan"),
                inOneHour, 3, now))
                .isInstanceOf(ActivityInvalidStateException.class);
    }

    @Test
    void update_rejected_when_startsAt_in_the_past_relative_to_provided_now() {
        Activity a = newActivity(5);
        Instant later = now.plus(2, ChronoUnit.HOURS);
        // Past relative to "later" should be rejected
        assertThatThrownBy(() -> a.update("New", null, Location.ofCity("Abidjan"),
                inOneHour, 5, later))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("startsAt");
    }

    @Test
    void is_open_for_booking_returns_false_when_not_published() {
        Activity a = newActivity(5);
        a.cancel();
        assertThat(a.isOpenForBooking(now)).isFalse();
    }

    private Activity newActivity(int capacity) {
        return Activity.create("Brunch", "Sunday brunch",
                Location.of("Abidjan", "Riviera 2", 5.3, -4.0),
                inOneHour, capacity, organizerId, now);
    }
}
