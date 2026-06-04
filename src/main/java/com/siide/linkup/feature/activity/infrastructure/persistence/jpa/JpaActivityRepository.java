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
    @Query("""
            SELECT a FROM Activity a
             WHERE a.organizerId = :organizerId
            """)
    Page<Activity> findByOrganizerId(@Param("organizerId") UUID organizerId, Pageable pageable);

    @Override
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Activity a
               SET a.bookedCount = a.bookedCount + :qty
             WHERE a.id = :id
               AND a.status = com.siide.linkup.feature.activity.domain.model.ActivityStatus.PUBLISHED
               AND a.startsAt > :now
               AND a.bookedCount + :qty <= a.capacity
            """)
    int reserveSeatsAtomic(@Param("id") UUID id, @Param("qty") int qty, @Param("now") Instant now);

    @Override
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Activity a
               SET a.bookedCount = a.bookedCount - :qty
             WHERE a.id = :id
               AND a.bookedCount >= :qty
            """)
    int releaseSeatsAtomic(@Param("id") UUID id, @Param("qty") int qty);
}
