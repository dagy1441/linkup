package com.siide.linkup.feature.profile.domain;

import com.siide.linkup.feature.profile.domain.model.Profile;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProfileRepository {

    Optional<Profile> findByUserId(UUID userId);

    Profile save(Profile profile);

    /** Pending-deletion profiles whose grace period has elapsed. Capped to avoid huge batches. */
    List<Profile> findExpiredDeletions(Instant now, int limit);

    /** Hard-delete the aggregate (and its element-collection rows via CASCADE). */
    void delete(Profile profile);
}
