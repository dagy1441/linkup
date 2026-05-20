package com.siide.linkup.feature.activity.domain;

import com.siide.linkup.feature.activity.domain.model.Activity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Domain port for {@link Activity} persistence. Query methods are kept minimal and
 * use-case-oriented; ad-hoc dynamic filtering belongs to the application layer.
 */
public interface ActivityRepository {

    Activity save(Activity activity);

    Optional<Activity> findById(UUID id);

    Page<Activity> findPublishedUpcoming(Instant now, Pageable pageable);

    Page<Activity> findPublishedUpcomingByCity(String city, Instant now, Pageable pageable);

    /**
     * Atomic, race-free seat reservation. Increments {@code booked_count} by {@code qty}
     * in a single SQL statement only when the activity is PUBLISHED, in the future, and
     * has at least {@code qty} remaining capacity. Returns the number of affected rows
     * (0 = could not reserve, 1 = success).
     */
    int reserveSeatsAtomic(UUID id, int qty, Instant now);

    /**
     * Atomic seat release. Decrements {@code booked_count} by {@code qty} only when the
     * current value is {@code >= qty} (defensive against double-release).
     */
    int releaseSeatsAtomic(UUID id, int qty);
}
