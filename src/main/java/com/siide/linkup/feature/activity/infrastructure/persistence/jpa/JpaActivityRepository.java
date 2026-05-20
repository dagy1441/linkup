package com.siide.linkup.feature.activity.infrastructure.persistence.jpa;

import com.siide.linkup.feature.activity.domain.ActivityRepository;
import com.siide.linkup.feature.activity.domain.model.Activity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface JpaActivityRepository extends JpaRepository<Activity, UUID>, ActivityRepository {

    @Override
    @Query("""
            SELECT a FROM Activity a
             WHERE a.status = com.siide.linkup.feature.activity.domain.model.ActivityStatus.PUBLISHED
               AND a.startsAt > :now
            """)
    Page<Activity> findPublishedUpcoming(@Param("now") Instant now, Pageable pageable);

    @Override
    @Query("""
            SELECT a FROM Activity a
             WHERE a.status = com.siide.linkup.feature.activity.domain.model.ActivityStatus.PUBLISHED
               AND a.startsAt > :now
               AND LOWER(a.location.city) = :city
            """)
    Page<Activity> findPublishedUpcomingByCity(@Param("city") String city,
                                               @Param("now") Instant now,
                                               Pageable pageable);

    @Override
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Activity a
               SET a.bookedCount = a.bookedCount + 1
             WHERE a.id = :id
               AND a.status = com.siide.linkup.feature.activity.domain.model.ActivityStatus.PUBLISHED
               AND a.startsAt > :now
               AND a.bookedCount < a.capacity
            """)
    int reserveSeatAtomic(@Param("id") UUID id, @Param("now") Instant now);

    @Override
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Activity a
               SET a.bookedCount = a.bookedCount - 1
             WHERE a.id = :id
               AND a.bookedCount > 0
            """)
    int releaseSeatAtomic(@Param("id") UUID id);
}
