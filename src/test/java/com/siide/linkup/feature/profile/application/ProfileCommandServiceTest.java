package com.siide.linkup.feature.profile.application;

import com.siide.linkup.feature.profile.application.dto.UpdateInterestsCommand;
import com.siide.linkup.feature.profile.application.dto.UpdateProfileCommand;
import com.siide.linkup.feature.profile.domain.InterestCatalog;
import com.siide.linkup.feature.profile.domain.ProfileRepository;
import com.siide.linkup.feature.profile.domain.event.ProfileCompletedEvent;
import com.siide.linkup.feature.profile.domain.model.Gender;
import com.siide.linkup.feature.profile.domain.model.Profile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProfileCommandServiceTest {

    private ProfileRepository repository;
    private InterestCatalog interestCatalog;
    private ApplicationEventPublisher events;
    private ProfileCommandService service;

    private final UUID userId = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-05-25T10:00:00Z");

    @BeforeEach
    void setUp() {
        repository = mock(ProfileRepository.class);
        interestCatalog = mock(InterestCatalog.class);
        events = mock(ApplicationEventPublisher.class);
        // save() returns its argument so the service sees the same instance back.
        when(repository.save(any(Profile.class))).thenAnswer(inv -> inv.getArgument(0));
        service = new ProfileCommandService(repository, interestCatalog, events,
                Clock.fixed(now, ZoneOffset.UTC));
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
