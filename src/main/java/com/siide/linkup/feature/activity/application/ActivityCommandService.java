package com.siide.linkup.feature.activity.application;

import com.siide.linkup.feature.activity.application.dto.CreateActivityCommand;
import com.siide.linkup.feature.activity.application.dto.UpdateActivityCommand;
import com.siide.linkup.feature.activity.domain.ActivityRepository;
import com.siide.linkup.feature.activity.domain.event.ActivityCancelledEvent;
import com.siide.linkup.feature.activity.domain.event.ActivityCreatedEvent;
import com.siide.linkup.feature.activity.domain.exception.ActivityAccessDeniedException;
import com.siide.linkup.feature.activity.domain.exception.ActivityNotFoundException;
import com.siide.linkup.feature.activity.domain.model.Activity;
import com.siide.linkup.feature.activity.domain.model.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public ActivityCommandService(ActivityRepository repository,
                                  ApplicationEventPublisher eventPublisher,
                                  Clock clock) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional
    public Activity create(CreateActivityCommand cmd, UUID organizerId) {
        Location location = Location.of(cmd.city(), cmd.addressLine(), cmd.latitude(), cmd.longitude());
        Activity activity = Activity.create(
                cmd.title(), cmd.description(), location, cmd.startsAt(), cmd.capacity(),
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
        activity.update(cmd.title(), cmd.description(), location, cmd.startsAt(), cmd.capacity(),
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

    private Activity loadAndAuthorize(UUID activityId, UUID currentUserId) {
        Activity activity = repository.findById(activityId)
                .orElseThrow(() -> new ActivityNotFoundException(activityId));
        if (!activity.isOrganizedBy(currentUserId)) {
            throw new ActivityAccessDeniedException(activityId, currentUserId);
        }
        return activity;
    }
}
