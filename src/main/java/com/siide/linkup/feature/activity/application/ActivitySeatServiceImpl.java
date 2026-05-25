package com.siide.linkup.feature.activity.application;

import com.siide.linkup.feature.activity.api.ActivitySeatService;
import com.siide.linkup.feature.activity.domain.ActivityRepository;
import com.siide.linkup.feature.activity.domain.exception.SeatReleaseFailedException;
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
    public boolean tryReserveSeats(UUID activityId, int qty) {
        requirePositive(qty);
        int updated = repository.reserveSeatsAtomic(activityId, qty, Instant.now(clock));
        if (updated == 0) {
            log.debug("Seat reservation rejected for activity {} qty={}", activityId, qty);
            return false;
        }
        log.debug("Reserved {} seat(s) on activity {}", qty, activityId);
        return true;
    }

    @Override
    @Transactional
    public void releaseSeats(UUID activityId, int qty) {
        requirePositive(qty);
        int updated = repository.releaseSeatsAtomic(activityId, qty);
        if (updated == 0) {
            // Activity missing OR booked_count < qty (data drift / double-cancel).
            // Throw so the caller's transaction rolls back — we refuse to mark a
            // booking cancelled while leaving its seats stuck on the activity.
            log.error("Seat release rejected for activity {} qty={} (booked count too low or activity missing)",
                    activityId, qty);
            throw new SeatReleaseFailedException(activityId, qty);
        }
        log.debug("Released {} seat(s) on activity {}", qty, activityId);
    }

    private static void requirePositive(int qty) {
        if (qty <= 0) {
            throw new IllegalArgumentException("qty must be > 0");
        }
    }
}
