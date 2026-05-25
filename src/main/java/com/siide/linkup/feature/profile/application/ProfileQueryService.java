package com.siide.linkup.feature.profile.application;

import com.siide.linkup.feature.profile.domain.ProfileRepository;
import com.siide.linkup.feature.profile.domain.exception.ProfileNotFoundException;
import com.siide.linkup.feature.profile.domain.model.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ProfileQueryService {

    private final ProfileRepository repository;

    public ProfileQueryService(ProfileRepository repository) {
        this.repository = repository;
    }

    /**
     * Read a profile by its owning userId. Used by the {@code GET /profile/me}
     * endpoint when the caller wants a strict 404 — the controller delegates to
     * {@link ProfileCommandService#ensureProfile} when it wants auto-provisioning.
     */
    public Profile getByUserId(UUID userId) {
        return repository.findByUserId(userId)
                .orElseThrow(() -> new ProfileNotFoundException(userId));
    }
}
