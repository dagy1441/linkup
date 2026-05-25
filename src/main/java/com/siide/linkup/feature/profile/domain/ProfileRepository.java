package com.siide.linkup.feature.profile.domain;

import com.siide.linkup.feature.profile.domain.model.Profile;

import java.util.Optional;
import java.util.UUID;

public interface ProfileRepository {

    Optional<Profile> findByUserId(UUID userId);

    Profile save(Profile profile);
}
