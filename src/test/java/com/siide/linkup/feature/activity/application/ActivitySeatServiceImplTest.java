package com.siide.linkup.feature.activity.application;

import com.siide.linkup.feature.activity.domain.ActivityRepository;
import com.siide.linkup.feature.activity.domain.exception.SeatReleaseFailedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ActivitySeatServiceImplTest {

    private ActivityRepository repository;
    private ActivitySeatServiceImpl service;
    private final Instant now = Instant.parse("2026-01-01T10:00:00Z");

    @BeforeEach
    void setUp() {
        repository = mock(ActivityRepository.class);
        service = new ActivitySeatServiceImpl(repository, Clock.fixed(now, ZoneOffset.UTC));
    }

    @Test
    void try_reserve_returns_true_when_row_updated() {
        UUID id = UUID.randomUUID();
        when(repository.reserveSeatsAtomic(eq(id), eq(3), any())).thenReturn(1);

        assertThat(service.tryReserveSeats(id, 3)).isTrue();
    }

    @Test
    void try_reserve_returns_false_when_no_row_updated() {
        UUID id = UUID.randomUUID();
        when(repository.reserveSeatsAtomic(eq(id), anyInt(), any())).thenReturn(0);

        assertThat(service.tryReserveSeats(id, 2)).isFalse();
    }

    @Test
    void release_throws_when_no_row_affected_so_caller_rolls_back() {
        UUID id = UUID.randomUUID();
        when(repository.releaseSeatsAtomic(id, 2)).thenReturn(0);

        // Activity missing OR booked_count < qty → refuse to leave seats stuck.
        assertThatThrownBy(() -> service.releaseSeats(id, 2))
                .isInstanceOf(SeatReleaseFailedException.class);
    }

    @Test
    void release_succeeds_when_row_affected() {
        UUID id = UUID.randomUUID();
        when(repository.releaseSeatsAtomic(id, 2)).thenReturn(1);

        service.releaseSeats(id, 2); // no exception
    }

    @Test
    void try_reserve_rejects_non_positive_qty() {
        assertThatThrownBy(() -> service.tryReserveSeats(UUID.randomUUID(), 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void release_rejects_non_positive_qty() {
        assertThatThrownBy(() -> service.releaseSeats(UUID.randomUUID(), -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
