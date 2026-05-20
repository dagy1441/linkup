package com.siide.linkup.feature.activity.application;

import com.siide.linkup.feature.activity.domain.ActivityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
    void try_reserve_returns_true_when_one_row_updated() {
        UUID id = UUID.randomUUID();
        when(repository.reserveSeatAtomic(eq(id), any())).thenReturn(1);

        assertThat(service.tryReserveSeat(id)).isTrue();
    }

    @Test
    void try_reserve_returns_false_when_no_row_updated() {
        UUID id = UUID.randomUUID();
        when(repository.reserveSeatAtomic(eq(id), any())).thenReturn(0);

        assertThat(service.tryReserveSeat(id)).isFalse();
    }

    @Test
    void release_does_not_throw_when_no_row_affected() {
        UUID id = UUID.randomUUID();
        when(repository.releaseSeatAtomic(id)).thenReturn(0);

        service.releaseSeat(id); // no exception
    }
}
