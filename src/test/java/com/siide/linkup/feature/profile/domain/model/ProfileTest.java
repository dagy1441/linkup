package com.siide.linkup.feature.profile.domain.model;

import com.siide.linkup.feature.profile.domain.exception.ProfileInvalidStateException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProfileTest {

    private final UUID userId = UUID.randomUUID();
    // Anchor "now" to a deterministic instant so age math is stable.
    private final Instant now = Instant.parse("2026-05-25T10:00:00Z");

    @Test
    void empty_profile_is_active_and_incomplete() {
        Profile p = Profile.empty(userId);

        assertThat(p.getUserId()).isEqualTo(userId);
        assertThat(p.getStatus()).isEqualTo(ProfileStatus.ACTIVE);
        assertThat(p.isComplete()).isFalse();
        assertThat(p.getId()).isNotNull();
    }

    @Test
    void update_trims_and_blanks_to_null() {
        Profile p = Profile.empty(userId);

        p.update("  Loves yoga  ", "  Abidjan  ", null, null, now);

        assertThat(p.getBio()).isEqualTo("Loves yoga");
        assertThat(p.getCity()).isEqualTo("Abidjan");
        assertThat(p.getDateOfBirth()).isNull();
        // Still incomplete because dateOfBirth is missing.
        assertThat(p.isComplete()).isFalse();
    }

    @Test
    void update_rejects_oversized_bio() {
        Profile p = Profile.empty(userId);
        String huge = "x".repeat(Profile.BIO_MAX_LENGTH + 1);

        assertThatThrownBy(() -> p.update(huge, null, null, null, now))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bio");
    }

    @Test
    void update_rejects_future_birth_date() {
        Profile p = Profile.empty(userId);
        LocalDate future = LocalDate.of(2099, 1, 1);

        assertThatThrownBy(() -> p.update(null, null, future, null, now))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("past");
    }

    @Test
    void update_rejects_user_below_min_age() {
        Profile p = Profile.empty(userId);
        // Today is 2026-05-25; a user born 2020-01-01 is 6 years old → below 13.
        LocalDate child = LocalDate.of(2020, 1, 1);

        assertThatThrownBy(() -> p.update(null, null, child, null, now))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("13");
    }

    @Test
    void profile_is_complete_when_bio_city_and_dob_set() {
        Profile p = Profile.empty(userId);

        p.update("Hi", "Abidjan", LocalDate.of(1995, 1, 1), Gender.UNDISCLOSED, now);

        assertThat(p.isComplete()).isTrue();
    }

    @Test
    void update_refused_when_pending_deletion() {
        Profile p = Profile.empty(userId);
        p.markForDeletion(now);

        assertThatThrownBy(() -> p.update("hi", "Abidjan", null, null, now))
                .isInstanceOf(ProfileInvalidStateException.class)
                .hasMessageContaining("pending deletion");
    }

    @Test
    void cancel_deletion_restores_active_state() {
        Profile p = Profile.empty(userId);
        p.markForDeletion(now);

        p.cancelDeletion();

        assertThat(p.getStatus()).isEqualTo(ProfileStatus.ACTIVE);
        assertThat(p.getDeletionScheduledAt()).isNull();
    }

    @Test
    void cancel_deletion_throws_when_not_pending() {
        Profile p = Profile.empty(userId);

        assertThatThrownBy(p::cancelDeletion)
                .isInstanceOf(ProfileInvalidStateException.class);
    }
}
