package com.siide.linkup.feature.activity.application;

import com.siide.linkup.feature.activity.domain.ActivityRepository;
import com.siide.linkup.feature.activity.domain.exception.ActivityNotFoundException;
import com.siide.linkup.feature.activity.domain.model.Activity;
import com.siide.linkup.feature.activity.domain.model.ActivityCategory;
import com.siide.linkup.feature.activity.domain.model.Location;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
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

class ActivityQueryServiceTest {

    private ActivityRepository repository;
    private ActivityQueryService service;
    private final Clock fixedClock = Clock.fixed(Instant.parse("2026-01-01T10:00:00Z"), ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        repository = mock(ActivityRepository.class);
        service = new ActivityQueryService(repository, new ActivityProperties(20, 100), fixedClock);
    }

    @Test
    void list_without_city_calls_unfiltered_query() {
        Page<Activity> page = new PageImpl<>(List.of(sampleActivity()));
        when(repository.findPublishedUpcoming(any(), any())).thenReturn(page);

        Page<Activity> result = service.listPublishedUpcoming(Optional.empty(), null, null);

        assertThat(result.getContent()).hasSize(1);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findPublishedUpcoming(eq(Instant.parse("2026-01-01T10:00:00Z")), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(20);
        assertThat(pageableCaptor.getValue().getPageNumber()).isZero();
    }

    @Test
    void list_with_city_uses_lowercased_filter() {
        Page<Activity> page = new PageImpl<>(List.of());
        when(repository.findPublishedUpcomingByCity(eq("abidjan"), any(), any())).thenReturn(page);

        service.listPublishedUpcoming(Optional.of(" Abidjan "), 1, 10);

        verify(repository).findPublishedUpcomingByCity(eq("abidjan"), any(), any());
    }

    @Test
    void list_caps_page_size_to_configured_max() {
        when(repository.findPublishedUpcoming(any(), any())).thenReturn(new PageImpl<>(List.of()));

        service.listPublishedUpcoming(Optional.empty(), 0, 9_999);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findPublishedUpcoming(any(), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(100);
    }

    @Test
    void list_mine_delegates_to_repository_with_organizer_id_and_desc_sort() {
        UUID organizerId = UUID.randomUUID();
        when(repository.findByOrganizerId(eq(organizerId), any())).thenReturn(new PageImpl<>(List.of()));

        service.listMine(organizerId, null, null);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findByOrganizerId(eq(organizerId), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(20);
        assertThat(captor.getValue().getPageNumber()).isZero();
        assertThat(captor.getValue().getSort().getOrderFor("startsAt"))
                .isNotNull()
                .extracting(o -> o.getDirection().name())
                .isEqualTo("DESC");
    }

    @Test
    void list_mine_caps_page_size() {
        UUID organizerId = UUID.randomUUID();
        when(repository.findByOrganizerId(eq(organizerId), any())).thenReturn(new PageImpl<>(List.of()));

        service.listMine(organizerId, 0, 9_999);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findByOrganizerId(eq(organizerId), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(100);
    }

    @Test
    void get_by_id_throws_when_missing() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(id))
                .isInstanceOf(ActivityNotFoundException.class);
    }

    private Activity sampleActivity() {
        Instant now = Instant.parse("2026-01-01T10:00:00Z");
        return Activity.create("Brunch", "d", ActivityCategory.CULTURE,
                Location.of("Abidjan", null, null, null),
                now.plus(1, ChronoUnit.HOURS),
                10, UUID.randomUUID(), now);
    }
}
