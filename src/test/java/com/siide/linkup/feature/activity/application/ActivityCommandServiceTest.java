package com.siide.linkup.feature.activity.application;

import com.siide.linkup.feature.activity.application.dto.CreateActivityCommand;
import com.siide.linkup.feature.activity.application.dto.UpdateActivityCommand;
import com.siide.linkup.feature.activity.domain.ActivityRepository;
import com.siide.linkup.feature.activity.domain.event.ActivityCancelledEvent;
import com.siide.linkup.feature.activity.domain.event.ActivityCreatedEvent;
import com.siide.linkup.feature.activity.domain.exception.ActivityAccessDeniedException;
import com.siide.linkup.feature.activity.domain.exception.ActivityNotFoundException;
import com.siide.linkup.feature.activity.domain.exception.InvalidCoverException;
import com.siide.linkup.feature.activity.domain.model.Activity;
import com.siide.linkup.feature.activity.domain.model.ActivityStatus;
import com.siide.linkup.feature.activity.domain.model.Location;
import com.siide.linkup.feature.activity.domain.storage.CoverStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.io.ByteArrayInputStream;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ActivityCommandServiceTest {

    private ActivityRepository repository;
    private CoverStorageService coverStorage;
    private ApplicationEventPublisher publisher;
    private ActivityCommandService service;

    private final UUID organizerId = UUID.randomUUID();
    private final UUID intruderId = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-01-01T10:00:00Z");
    private final Instant inOneHour = now.plus(1, ChronoUnit.HOURS);
    private final Clock fixedClock = Clock.fixed(now, ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        repository = mock(ActivityRepository.class);
        coverStorage = mock(CoverStorageService.class);
        publisher = mock(ApplicationEventPublisher.class);
        ActivityCoverProperties coverProps = new ActivityCoverProperties(
                1024L,                                          // tight 1 KB cap for tests
                List.of("image/jpeg", "image/png", "image/webp"),
                "linkup-activity-covers",
                Duration.ofHours(1));
        service = new ActivityCommandService(repository, coverStorage, coverProps, publisher, fixedClock);
        when(repository.save(any(Activity.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void create_persists_and_publishes_event() {
        CreateActivityCommand cmd = new CreateActivityCommand(
                "Brunch", "desc", "Abidjan", "Riviera", 5.3, -4.0, inOneHour, 10);

        Activity result = service.create(cmd, organizerId);

        assertThat(result.getTitle()).isEqualTo("Brunch");
        assertThat(result.getStatus()).isEqualTo(ActivityStatus.PUBLISHED);
        verify(publisher).publishEvent(any(ActivityCreatedEvent.class));
    }

    @Test
    void update_by_organizer_updates_fields() {
        Activity existing = baseActivity();
        when(repository.findById(existing.getId())).thenReturn(Optional.of(existing));

        UpdateActivityCommand cmd = new UpdateActivityCommand(
                "Brunch v2", "new desc", "Abidjan", null, null, null, inOneHour, 20);
        Activity updated = service.update(existing.getId(), cmd, organizerId);

        assertThat(updated.getTitle()).isEqualTo("Brunch v2");
        assertThat(updated.getCapacity()).isEqualTo(20);
    }

    @Test
    void update_rejected_when_caller_is_not_organizer() {
        Activity existing = baseActivity();
        when(repository.findById(existing.getId())).thenReturn(Optional.of(existing));

        UpdateActivityCommand cmd = new UpdateActivityCommand(
                "x", null, "Abidjan", null, null, null, inOneHour, 10);
        assertThatThrownBy(() -> service.update(existing.getId(), cmd, intruderId))
                .isInstanceOf(ActivityAccessDeniedException.class);
    }

    @Test
    void update_rejected_when_activity_missing() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        UpdateActivityCommand cmd = new UpdateActivityCommand(
                "x", null, "Abidjan", null, null, null, inOneHour, 10);
        assertThatThrownBy(() -> service.update(id, cmd, organizerId))
                .isInstanceOf(ActivityNotFoundException.class);
    }

    @Test
    void cancel_publishes_cancelled_event() {
        Activity existing = baseActivity();
        when(repository.findById(existing.getId())).thenReturn(Optional.of(existing));

        Activity cancelled = service.cancel(existing.getId(), organizerId);

        assertThat(cancelled.getStatus()).isEqualTo(ActivityStatus.CANCELLED);
        verify(publisher).publishEvent(any(ActivityCancelledEvent.class));
    }

    @Test
    void cancel_rejected_when_caller_is_not_organizer() {
        Activity existing = baseActivity();
        when(repository.findById(existing.getId())).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.cancel(existing.getId(), intruderId))
                .isInstanceOf(ActivityAccessDeniedException.class);
        verify(publisher, never()).publishEvent(any());
    }

    @Test
    void upload_cover_rejects_unsupported_content_type() {
        Activity activity = baseActivity();
        when(repository.findById(activity.getId())).thenReturn(Optional.of(activity));

        assertThatThrownBy(() -> service.uploadCover(activity.getId(), organizerId,
                "image/gif", 100L, new ByteArrayInputStream(new byte[100])))
                .isInstanceOf(InvalidCoverException.class)
                .hasMessageContaining("unsupported content-type");
    }

    @Test
    void upload_cover_rejects_oversized_payload() {
        Activity activity = baseActivity();
        when(repository.findById(activity.getId())).thenReturn(Optional.of(activity));

        // setUp() configured maxBytes=1024
        assertThatThrownBy(() -> service.uploadCover(activity.getId(), organizerId,
                "image/jpeg", 2048L, new ByteArrayInputStream(new byte[2048])))
                .isInstanceOf(InvalidCoverException.class)
                .hasMessageContaining("exceeds");
    }

    @Test
    void upload_cover_refused_when_caller_is_not_the_organizer() {
        Activity activity = baseActivity();
        when(repository.findById(activity.getId())).thenReturn(Optional.of(activity));

        assertThatThrownBy(() -> service.uploadCover(activity.getId(), intruderId,
                "image/jpeg", 100L, new ByteArrayInputStream(new byte[100])))
                .isInstanceOf(ActivityAccessDeniedException.class);
    }

    @Test
    void upload_cover_attaches_new_key_and_deletes_previous() {
        Activity activity = baseActivity();
        activity.attachCover("activities/" + activity.getId() + "/cover.old");
        when(repository.findById(activity.getId())).thenReturn(Optional.of(activity));
        when(coverStorage.upload(any(), any(), anyLong(), any()))
                .thenReturn("activities/" + activity.getId() + "/cover.jpg");

        Activity result = service.uploadCover(activity.getId(), organizerId,
                "image/jpeg", 100L, new ByteArrayInputStream(new byte[100]));

        assertThat(result.getCoverKey()).endsWith("cover.jpg");
        verify(coverStorage).delete("activities/" + activity.getId() + "/cover.old");
    }

    @Test
    void remove_cover_is_noop_when_none_attached() {
        Activity activity = baseActivity();
        when(repository.findById(activity.getId())).thenReturn(Optional.of(activity));

        Activity result = service.removeCover(activity.getId(), organizerId);

        assertThat(result.getCoverKey()).isNull();
        verify(coverStorage, never()).delete(any());
    }

    private Activity baseActivity() {
        return Activity.create("Brunch", "desc",
                Location.of("Abidjan", "Riviera", 5.3, -4.0),
                inOneHour, 10, organizerId, now);
    }
}
