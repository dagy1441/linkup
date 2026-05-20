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
     * Atomic, race-free seat reservation. Increments {@code booked_count} in a single
     * SQL statement only when the activity is PUBLISHED, in the future, and has remaining
     * capacity. Returns the number of affected rows (0 = could not reserve).
     */
    int reserveSeatAtomic(UUID id, Instant now);

    int releaseSeatAtomic(UUID id);
}
