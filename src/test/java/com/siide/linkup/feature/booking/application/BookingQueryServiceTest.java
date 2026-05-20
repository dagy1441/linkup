package com.siide.linkup.feature.booking.application;

import com.siide.linkup.feature.booking.domain.BookingRepository;
import com.siide.linkup.feature.booking.domain.exception.BookingAccessDeniedException;
import com.siide.linkup.feature.booking.domain.exception.BookingNotFoundException;
import com.siide.linkup.feature.booking.domain.model.Booking;
import com.siide.linkup.feature.booking.domain.model.BookingStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookingQueryServiceTest {

    private BookingRepository repository;
    private BookingQueryService service;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        repository = mock(BookingRepository.class);
        service = new BookingQueryService(repository, new BookingProperties(5, 20, 100));
    }

    @Test
    void get_owned_returns_booking_when_owner_matches() {
        Booking booking = Booking.confirm(userId, UUID.randomUUID(), 1);
        when(repository.findById(booking.getId())).thenReturn(Optional.of(booking));

        assertThat(service.getOwnedById(booking.getId(), userId)).isSameAs(booking);
    }

    @Test
    void get_owned_throws_when_not_found() {
        UUID bookingId = UUID.randomUUID();
        when(repository.findById(bookingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getOwnedById(bookingId, userId))
                .isInstanceOf(BookingNotFoundException.class);
    }

    @Test
    void get_owned_throws_when_user_is_not_owner() {
        Booking booking = Booking.confirm(userId, UUID.randomUUID(), 1);
        when(repository.findById(booking.getId())).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> service.getOwnedById(booking.getId(), UUID.randomUUID()))
                .isInstanceOf(BookingAccessDeniedException.class);
    }

    @Test
    void list_mine_without_status_calls_unfiltered_repo() {
        Page<Booking> page = new PageImpl<>(List.of(Booking.confirm(userId, UUID.randomUUID(), 1)));
        when(repository.findByUserId(eq(userId), any(Pageable.class))).thenReturn(page);

        Page<Booking> result = service.listMine(userId, Optional.empty(), null, null);

        assertThat(result.getContent()).hasSize(1);
        verify(repository).findByUserId(eq(userId), any(Pageable.class));
    }

    @Test
    void list_mine_with_status_calls_filtered_repo() {
        Page<Booking> page = new PageImpl<>(List.of());
        when(repository.findByUserIdAndStatus(eq(userId), eq(BookingStatus.CONFIRMED), any(Pageable.class)))
                .thenReturn(page);

        service.listMine(userId, Optional.of(BookingStatus.CONFIRMED), 0, 10);

        verify(repository).findByUserIdAndStatus(eq(userId), eq(BookingStatus.CONFIRMED), any(Pageable.class));
    }

    @Test
    void list_mine_caps_size_at_max_page_size() {
        Page<Booking> page = new PageImpl<>(List.of());
        when(repository.findByUserId(eq(userId), any(Pageable.class))).thenReturn(page);

        service.listMine(userId, Optional.empty(), null, 500);

        verify(repository).findByUserId(eq(userId), org.mockito.ArgumentMatchers.argThat(
                p -> p.getPageSize() == 100));
    }
}
