package com.siide.linkup.feature.profile.application;

import com.siide.linkup.feature.profile.application.dto.UpdateInterestsCommand;
import com.siide.linkup.feature.profile.application.dto.UpdateProfileCommand;
import com.siide.linkup.feature.profile.domain.InterestCatalog;
import com.siide.linkup.feature.profile.domain.ProfileRepository;
import com.siide.linkup.feature.profile.domain.event.ProfileCompletedEvent;
import com.siide.linkup.feature.profile.domain.exception.InvalidPhotoException;
import com.siide.linkup.feature.profile.domain.model.Gender;
import com.siide.linkup.feature.profile.domain.model.Profile;
import com.siide.linkup.feature.profile.domain.storage.PhotoStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.io.ByteArrayInputStream;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProfileCommandServiceTest {

    private ProfileRepository repository;
    private InterestCatalog interestCatalog;
    private PhotoStorageService photoStorage;
    private ApplicationEventPublisher events;
    private ProfileCommandService service;

    private final UUID userId = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-05-25T10:00:00Z");

    @BeforeEach
    void setUp() {
        repository = mock(ProfileRepository.class);
        interestCatalog = mock(InterestCatalog.class);
        photoStorage = mock(PhotoStorageService.class);
        events = mock(ApplicationEventPublisher.class);
        // save() returns its argument so the service sees the same instance back.
        when(repository.save(any(Profile.class))).thenAnswer(inv -> inv.getArgument(0));
        ProfileStorageProperties props = new ProfileStorageProperties(
                1024L,                                          // tight 1 KB cap for tests
                List.of("image/jpeg", "image/png", "image/webp"),
                new ProfileStorageProperties.Storage(
                        "http://localhost:9000", "ak", "sk", "linkup-profiles", Duration.ofHours(1)));
        ProfileLifecycleProperties lifecycle = new ProfileLifecycleProperties(
                Duration.ofDays(30), "0 0 4 * * *");
        service = new ProfileCommandService(repository, interestCatalog, photoStorage, props, lifecycle,
                events, Clock.fixed(now, ZoneOffset.UTC));
    }

    @Test
    void ensure_profile_creates_empty_when_missing() {
        when(repository.findByUserId(userId)).thenReturn(Optional.empty());

        Profile result = service.ensureProfile(userId);

        assertThat(result.getUserId()).isEqualTo(userId);
        verify(repository).save(any(Profile.class));
    }

    @Test
    void ensure_profile_returns_existing_without_save() {
        Profile existing = Profile.empty(userId);
        when(repository.findByUserId(userId)).thenReturn(Optional.of(existing));

        Profile result = service.ensureProfile(userId);

        assertThat(result).isSameAs(existing);
        verify(repository, never()).save(any(Profile.class));
    }

    @Test
    void update_does_not_fire_event_when_interests_missing() {
        // After PR #6 isComplete requires bio + city + DOB + at least 1 interest.
        // A bare PUT /me with only bio/city/DOB is no longer "complete".
        Profile profile = Profile.empty(userId);
        when(repository.findByUserId(userId)).thenReturn(Optional.of(profile));

        UpdateProfileCommand cmd = new UpdateProfileCommand(
                "Loves yoga", "Abidjan", LocalDate.of(1995, 1, 1), Gender.UNDISCLOSED);
        service.update(userId, cmd);

        verify(events, never()).publishEvent(any(ProfileCompletedEvent.class));
    }

    @Test
    void update_interests_fires_completed_event_when_it_closes_the_last_gap() {
        // Profile already has bio + city + DOB but no interests.
        Profile profile = Profile.empty(userId);
        profile.update("hi", "Abidjan", LocalDate.of(1995, 1, 1), Gender.MALE, now);
        when(repository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(interestCatalog.filterValidSlugs(any())).thenReturn(Set.of("yoga"));

        service.updateInterests(userId, new UpdateInterestsCommand(Set.of("yoga", "unknown-slug")));

        verify(events).publishEvent(any(ProfileCompletedEvent.class));
    }

    @Test
    void upload_photo_rejects_unsupported_content_type() {
        when(repository.findByUserId(userId)).thenReturn(Optional.of(Profile.empty(userId)));

        assertThatThrownBy(() -> service.uploadPhoto(userId, "image/gif", 100,
                new ByteArrayInputStream(new byte[100])))
                .isInstanceOf(InvalidPhotoException.class)
                .hasMessageContaining("unsupported content-type");
    }

    @Test
    void upload_photo_rejects_oversized_payload() {
        when(repository.findByUserId(userId)).thenReturn(Optional.of(Profile.empty(userId)));

        // setUp() configured maxBytes=1024
        assertThatThrownBy(() -> service.uploadPhoto(userId, "image/jpeg", 2048,
                new ByteArrayInputStream(new byte[2048])))
                .isInstanceOf(InvalidPhotoException.class)
                .hasMessageContaining("exceeds");
    }

    @Test
    void upload_photo_attaches_new_key_and_deletes_previous() {
        Profile profile = Profile.empty(userId);
        profile.attachPhoto("profiles/" + profile.getId() + "/avatar.old");
        when(repository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(photoStorage.upload(any(), any(), anyLong(), any()))
                .thenReturn("profiles/" + profile.getId() + "/avatar.jpg");

        Profile result = service.uploadPhoto(userId, "image/jpeg", 100,
                new ByteArrayInputStream(new byte[100]));

        assertThat(result.getPhotoKey()).endsWith("avatar.jpg");
        verify(photoStorage).delete("profiles/" + profile.getId() + "/avatar.old");
    }

    @Test
    void remove_photo_is_noop_when_none_attached() {
        Profile profile = Profile.empty(userId);
        when(repository.findByUserId(userId)).thenReturn(Optional.of(profile));

        Profile result = service.removePhoto(userId);

        assertThat(result.getPhotoKey()).isNull();
        verify(photoStorage, never()).delete(any());
    }

    @Test
    void request_deletion_marks_profile_and_schedules_purge() {
        Profile profile = Profile.empty(userId);
        when(repository.findByUserId(userId)).thenReturn(Optional.of(profile));

        Profile result = service.requestDeletion(userId);

        assertThat(result.getStatus().name()).isEqualTo("DELETION_PENDING");
        assertThat(result.getDeletionScheduledAt()).isEqualTo(now.plus(Duration.ofDays(30)));
        verify(events).publishEvent(any(
                com.siide.linkup.feature.profile.domain.event.ProfileDeletionRequestedEvent.class));
    }

    @Test
    void restore_deletion_brings_profile_back_to_active() {
        Profile profile = Profile.empty(userId);
        profile.markForDeletion(now.plus(Duration.ofDays(30)));
        when(repository.findByUserId(userId)).thenReturn(Optional.of(profile));

        Profile result = service.restoreFromDeletion(userId);

        assertThat(result.getStatus().name()).isEqualTo("ACTIVE");
        assertThat(result.getDeletionScheduledAt()).isNull();
    }

    @Test
    void restore_deletion_throws_when_profile_is_active() {
        Profile profile = Profile.empty(userId);
        when(repository.findByUserId(userId)).thenReturn(Optional.of(profile));

        assertThatThrownBy(() -> service.restoreFromDeletion(userId))
                .isInstanceOf(com.siide.linkup.feature.profile.domain.exception.ProfileInvalidStateException.class);
    }

    @Test
    void purge_deletes_profile_and_emits_event_when_ready() {
        Profile profile = Profile.empty(userId);
        profile.attachPhoto("profiles/" + profile.getId() + "/avatar.jpg"); // BEFORE markForDeletion (mutates only when ACTIVE)
        profile.markForDeletion(now.minus(Duration.ofMinutes(1))); // already expired

        service.purge(profile);

        verify(photoStorage).delete("profiles/" + profile.getId() + "/avatar.jpg");
        verify(events).publishEvent(any(
                com.siide.linkup.feature.profile.domain.event.ProfilePurgedEvent.class));
        verify(repository).delete(profile);
    }

    @Test
    void purge_is_skipped_when_profile_was_restored_between_scan_and_call() {
        // Scheduler picked it up while DELETION_PENDING, but user restored before purge ran.
        Profile profile = Profile.empty(userId); // status = ACTIVE

        service.purge(profile);

        verify(repository, never()).delete(any());
        verify(events, never()).publishEvent(any(
                com.siide.linkup.feature.profile.domain.event.ProfilePurgedEvent.class));
    }

    @Test
    void update_interests_drops_unknown_slugs_via_catalog() {
        Profile profile = Profile.empty(userId);
        when(repository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(interestCatalog.filterValidSlugs(any())).thenReturn(Set.of("yoga"));

        Profile result = service.updateInterests(userId,
                new UpdateInterestsCommand(Set.of("yoga", "ghost", "void")));

        assertThat(result.getInterestSlugs()).containsExactly("yoga");
    }

    @Test
    void update_does_not_fire_event_on_subsequent_completed_updates() {
        // Already complete (bio + city + DOB + interest). A second update must NOT re-fire.
        Profile profile = Profile.empty(userId);
        profile.update("first bio", "Abidjan", LocalDate.of(1995, 1, 1), Gender.MALE, now);
        profile.replaceInterests(Set.of("yoga"));
        when(repository.findByUserId(userId)).thenReturn(Optional.of(profile));

        UpdateProfileCommand cmd = new UpdateProfileCommand(
                "second bio", "Abidjan", LocalDate.of(1995, 1, 1), Gender.MALE);
        service.update(userId, cmd);

        verify(events, never()).publishEvent(any(ProfileCompletedEvent.class));
    }

    @Test
    void update_does_not_fire_event_when_still_incomplete() {
        Profile profile = Profile.empty(userId);
        when(repository.findByUserId(userId)).thenReturn(Optional.of(profile));

        // Only bio + city, no DOB → still incomplete.
        UpdateProfileCommand cmd = new UpdateProfileCommand("bio", "Abidjan", null, null);
        service.update(userId, cmd);

        verify(events, never()).publishEvent(any(ProfileCompletedEvent.class));
    }
}
