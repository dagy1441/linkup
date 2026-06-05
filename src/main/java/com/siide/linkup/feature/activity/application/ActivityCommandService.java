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
import com.siide.linkup.feature.activity.domain.model.Location;
import com.siide.linkup.feature.activity.domain.storage.CoverStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Write-side use-cases for activities. All organizer-permission checks happen here.
 * Queries live in {@link ActivityQueryService}.
 */
@Service
public class ActivityCommandService {

    private static final Logger log = LoggerFactory.getLogger(ActivityCommandService.class);

    private final ActivityRepository repository;
    private final CoverStorageService coverStorage;
    private final ActivityCoverProperties coverProperties;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public ActivityCommandService(ActivityRepository repository,
                                  CoverStorageService coverStorage,
                                  ActivityCoverProperties coverProperties,
                                  ApplicationEventPublisher eventPublisher,
                                  Clock clock) {
        this.repository = repository;
        this.coverStorage = coverStorage;
        this.coverProperties = coverProperties;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional
    public Activity create(CreateActivityCommand cmd, UUID organizerId) {
        Location location = Location.of(cmd.city(), cmd.addressLine(), cmd.latitude(), cmd.longitude());
        Activity activity = Activity.create(
                cmd.title(), cmd.description(), cmd.category(), location, cmd.startsAt(), cmd.capacity(),
                organizerId, Instant.now(clock));
        Activity saved = repository.save(activity);
        eventPublisher.publishEvent(ActivityCreatedEvent.of(
                saved.getId(), organizerId, saved.getTitle(), saved.getStartsAt(), saved.getCapacity()));
        log.info("Activity created id={} organizerId={} capacity={}",
                saved.getId(), organizerId, saved.getCapacity());
        return saved;
    }

    @Transactional
    public Activity update(UUID activityId, UpdateActivityCommand cmd, UUID currentUserId) {
        Activity activity = loadAndAuthorize(activityId, currentUserId);
        Location location = Location.of(cmd.city(), cmd.addressLine(), cmd.latitude(), cmd.longitude());
        activity.update(cmd.title(), cmd.description(), cmd.category(), location, cmd.startsAt(), cmd.capacity(),
                Instant.now(clock));
        log.info("Activity updated id={} by userId={}", activityId, currentUserId);
        return activity;
    }

    @Transactional
    public Activity cancel(UUID activityId, UUID currentUserId) {
        Activity activity = loadAndAuthorize(activityId, currentUserId);
        activity.cancel();
        eventPublisher.publishEvent(ActivityCancelledEvent.of(activity.getId(), currentUserId));
        log.info("Activity cancelled id={} by userId={}", activityId, currentUserId);
        return activity;
    }

    /**
     * Replace (or set) the cover image. Validates content-type + size, uploads
     * to S3-compatible storage, then attaches the new key. The previous key
     * (if any) is deleted AFTER the new one is committed so a partial failure
     * doesn't leave the activity coverless.
     */
    @Transactional
    public Activity uploadCover(UUID activityId, UUID currentUserId,
                                String contentType, long size, InputStream data) {
        Activity activity = loadAndAuthorize(activityId, currentUserId);
        if (contentType == null
                || !coverProperties.allowedContentTypeSet().contains(contentType.toLowerCase())) {
            throw new InvalidCoverException(
                    "unsupported content-type (allowed: " + coverProperties.allowedContentTypes() + ")");
        }
        if (size <= 0) {
            throw new InvalidCoverException("empty payload");
        }
        if (size > coverProperties.maxBytes()) {
            throw new InvalidCoverException(
                    "payload exceeds " + coverProperties.maxBytes() + " bytes (got " + size + ")");
        }

        String previousKey = activity.getCoverKey();
        String newKey = coverStorage.upload(activity.getId(), contentType.toLowerCase(), size, data);
        activity.attachCover(newKey);

        if (previousKey != null && !previousKey.equals(newKey)) {
            try {
                coverStorage.delete(previousKey);
            } catch (RuntimeException e) {
                log.warn("Could not delete previous cover key={} (new key={} already attached)",
                        previousKey, newKey, e);
            }
        }
        log.info("Activity cover uploaded id={} userId={} key={}",
                activity.getId(), currentUserId, newKey);
        return activity;
    }

    /** Remove the cover (idempotent). */
    @Transactional
    public Activity removeCover(UUID activityId, UUID currentUserId) {
        Activity activity = loadAndAuthorize(activityId, currentUserId);
        String key = activity.getCoverKey();
        if (key == null) return activity;
        activity.clearCover();
        try {
            coverStorage.delete(key);
        } catch (RuntimeException e) {
            // Aggregate already updated — log + continue. Janitor sweep (Phase H) catches orphans.
            log.warn("Could not delete cover key={} for activity id={} -- orphaned in storage",
                    key, activity.getId(), e);
        }
        log.info("Activity cover removed id={} userId={}", activity.getId(), currentUserId);
        return activity;
    }

    private Activity loadAndAuthorize(UUID activityId, UUID currentUserId) {
        Activity activity = repository.findById(activityId)
                .orElseThrow(() -> new ActivityNotFoundException(activityId));
        if (!activity.isOrganizedBy(currentUserId)) {
            throw new ActivityAccessDeniedException(activityId, currentUserId);
        }
        return activity;
    }
}
