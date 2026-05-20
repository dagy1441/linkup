package com.siide.linkup.feature.activity.application;

import com.siide.linkup.feature.activity.api.ActivitySeatService;
import com.siide.linkup.feature.activity.domain.ActivityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class ActivitySeatServiceImpl implements ActivitySeatService {

    private static final Logger log = LoggerFactory.getLogger(ActivitySeatServiceImpl.class);

    private final ActivityRepository repository;
    private final Clock clock;

    public ActivitySeatServiceImpl(ActivityRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public boolean tryReserveSeat(UUID activityId) {
        int updated = repository.reserveSeatAtomic(activityId, Instant.now(clock));
        if (updated == 0) {
            log.debug("Seat reservation rejected for activity {}", activityId);
            return false;
        }
        log.debug("Seat reserved on activity {}", activityId);
        return true;
    }

    @Override
    @Transactional
    public void releaseSeat(UUID activityId) {
        int updated = repository.releaseSeatAtomic(activityId);
        if (updated == 0) {
            log.warn("Release seat had no effect for activity {} (booked count already 0 or activity missing)",
                    activityId);
        }
    }
}
