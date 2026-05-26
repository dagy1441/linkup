package com.siide.linkup.feature.profile.infrastructure.rest.dto;

import com.siide.linkup.feature.profile.domain.model.Gender;
import com.siide.linkup.feature.profile.domain.model.Profile;
import com.siide.linkup.feature.profile.domain.model.ProfileStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public record ProfileResponse(
        UUID id,
        UUID userId,
        String bio,
        String city,
        LocalDate dateOfBirth,
        Gender gender,
        /** Internal storage key — kept for ops / migration, not for UI consumption. */
        String photoKey,
        /** Time-limited presigned URL the browser can <img src> directly. {@code null} if no photo. */
        String photoUrl,
        Set<String> interests,
        boolean complete,
        ProfileStatus status,
        Instant deletionScheduledAt
) {
    public static ProfileResponse from(Profile p, String photoUrl) {
        return new ProfileResponse(
                p.getId(),
                p.getUserId(),
                p.getBio(),
                p.getCity(),
                p.getDateOfBirth(),
                p.getGender(),
                p.getPhotoKey(),
                photoUrl,
                p.getInterestSlugs(),
                p.isComplete(),
                p.getStatus(),
                p.getDeletionScheduledAt()
        );
    }

    /** Overload kept for callers that don't have a presigned URL on hand (e.g. tests). */
    public static ProfileResponse from(Profile p) {
        return from(p, null);
    }
}
