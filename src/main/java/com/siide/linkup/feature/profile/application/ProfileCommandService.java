package com.siide.linkup.feature.profile.application;

import com.siide.linkup.feature.profile.application.dto.UpdateProfileCommand;
import com.siide.linkup.feature.profile.domain.ProfileRepository;
import com.siide.linkup.feature.profile.domain.event.ProfileCompletedEvent;
import com.siide.linkup.feature.profile.domain.model.Profile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Write-side use cases for profiles. Idempotently materialises the profile on
 * first contact (mirrors how {@code feature.auth} provisions the User), then
 * applies updates and fires {@link ProfileCompletedEvent} the first time the
 * profile becomes complete.
 */
@Service
public class ProfileCommandService {

    private static final Logger log = LoggerFactory.getLogger(ProfileCommandService.class);

    private final ProfileRepository repository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public ProfileCommandService(ProfileRepository repository,
                                 ApplicationEventPublisher eventPublisher,
                                 Clock clock) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    /**
     * Get the profile of {@code userId}, creating an empty one if it doesn't exist
     * yet. Idempotent: re-running for the same user returns the same row.
     */
    @Transactional
    public Profile ensureProfile(UUID userId) {
        return repository.findByUserId(userId)
                .orElseGet(() -> {
                    Profile created = repository.save(Profile.empty(userId));
                    log.info("Provisioned empty profile id={} userId={}", created.getId(), userId);
                    return created;
                });
    }

    /**
     * Apply user-provided changes. If the profile transitions from "incomplete"
     * to "complete" during this update, fire {@link ProfileCompletedEvent} once.
     */
    @Transactional
    public Profile update(UUID userId, UpdateProfileCommand cmd) {
        Profile profile = ensureProfile(userId);
        boolean wasComplete = profile.isComplete();
        profile.update(cmd.bio(), cmd.city(), cmd.dateOfBirth(), cmd.gender(), Instant.now(clock));
        Profile saved = repository.save(profile);

        if (!wasComplete && saved.isComplete()) {
            Instant now = Instant.now(clock);
            eventPublisher.publishEvent(ProfileCompletedEvent.of(saved.getId(), userId, now));
            log.info("Profile completed id={} userId={}", saved.getId(), userId);
        } else {
            log.info("Profile updated id={} userId={}", saved.getId(), userId);
        }
        return saved;
    }
}
