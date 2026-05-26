package com.siide.linkup.feature.profile.application;

import com.siide.linkup.feature.profile.application.dto.UpdateInterestsCommand;
import com.siide.linkup.feature.profile.application.dto.UpdateProfileCommand;
import com.siide.linkup.feature.profile.domain.InterestCatalog;
import com.siide.linkup.feature.profile.domain.ProfileRepository;
import com.siide.linkup.feature.profile.domain.event.ProfileCompletedEvent;
import com.siide.linkup.feature.profile.domain.exception.InvalidPhotoException;
import com.siide.linkup.feature.profile.domain.model.Profile;
import com.siide.linkup.feature.profile.domain.storage.PhotoStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.util.Set;
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
    private final InterestCatalog interestCatalog;
    private final PhotoStorageService photoStorage;
    private final ProfileStorageProperties storageProperties;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public ProfileCommandService(ProfileRepository repository,
                                 InterestCatalog interestCatalog,
                                 PhotoStorageService photoStorage,
                                 ProfileStorageProperties storageProperties,
                                 ApplicationEventPublisher eventPublisher,
                                 Clock clock) {
        this.repository = repository;
        this.interestCatalog = interestCatalog;
        this.photoStorage = photoStorage;
        this.storageProperties = storageProperties;
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

        fireIfNewlyCompleted(saved, wasComplete, userId, "updated");
        return saved;
    }

    /**
     * Replace the user's interests with the given catalogue slugs. Silently drops
     * unknown / disabled slugs (caller can compare sizes if it wants to surface).
     * Triggers {@link ProfileCompletedEvent} if completion is reached now.
     */
    @Transactional
    public Profile updateInterests(UUID userId, UpdateInterestsCommand cmd) {
        Profile profile = ensureProfile(userId);
        boolean wasComplete = profile.isComplete();
        Set<String> validSlugs = interestCatalog.filterValidSlugs(cmd.slugs());
        profile.replaceInterests(validSlugs);
        Profile saved = repository.save(profile);
        fireIfNewlyCompleted(saved, wasComplete, userId, "interests-updated");
        return saved;
    }

    /**
     * Replace (or set) the profile photo. Validates content-type + size against
     * {@link ProfileStorageProperties}, uploads to MinIO, then attaches the new
     * key. The previous key (if any) is deleted AFTER the new one is committed
     * to avoid leaving the profile without a photo on a partial failure.
     */
    @Transactional
    public Profile uploadPhoto(UUID userId, String contentType, long size, InputStream data) {
        if (contentType == null || !storageProperties.allowedContentTypeSet().contains(contentType.toLowerCase())) {
            throw new InvalidPhotoException(
                    "unsupported content-type (allowed: " + storageProperties.allowedContentTypes() + ")");
        }
        if (size <= 0) {
            throw new InvalidPhotoException("empty payload");
        }
        if (size > storageProperties.maxBytes()) {
            throw new InvalidPhotoException(
                    "payload exceeds " + storageProperties.maxBytes() + " bytes (got " + size + ")");
        }

        Profile profile = ensureProfile(userId);
        String previousKey = profile.getPhotoKey();
        String newKey = photoStorage.upload(profile.getId(), contentType.toLowerCase(), size, data);
        profile.attachPhoto(newKey);
        Profile saved = repository.save(profile);

        // Best-effort: drop the old object after the new one is committed.
        if (previousKey != null && !previousKey.equals(newKey)) {
            try {
                photoStorage.delete(previousKey);
            } catch (RuntimeException e) {
                log.warn("Could not delete previous photo key={} (new key={} already attached)",
                        previousKey, newKey, e);
            }
        }
        log.info("Profile photo uploaded id={} userId={} key={}", saved.getId(), userId, newKey);
        return saved;
    }

    /** Remove the photo (idempotent). */
    @Transactional
    public Profile removePhoto(UUID userId) {
        Profile profile = ensureProfile(userId);
        String key = profile.getPhotoKey();
        if (key == null) {
            return profile;
        }
        profile.clearPhoto();
        Profile saved = repository.save(profile);
        try {
            photoStorage.delete(key);
        } catch (RuntimeException e) {
            // Aggregate already updated — log + continue. Janitor sweep (Phase H) will catch orphans.
            log.warn("Could not delete photo key={} for profile id={} — orphaned in storage",
                    key, profile.getId(), e);
        }
        log.info("Profile photo removed id={} userId={}", saved.getId(), userId);
        return saved;
    }

    private void fireIfNewlyCompleted(Profile saved, boolean wasComplete, UUID userId, String op) {
        if (!wasComplete && saved.isComplete()) {
            Instant now = Instant.now(clock);
            eventPublisher.publishEvent(ProfileCompletedEvent.of(saved.getId(), userId, now));
            log.info("Profile completed id={} userId={} via op={}", saved.getId(), userId, op);
        } else {
            log.info("Profile {} id={} userId={}", op, saved.getId(), userId);
        }
    }
}
