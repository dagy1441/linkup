package com.siide.linkup.feature.profile.infrastructure.persistence.jpa;

import com.siide.linkup.feature.profile.domain.ProfileRepository;
import com.siide.linkup.feature.profile.domain.model.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface JpaProfileRepository extends JpaRepository<Profile, UUID>, ProfileRepository {

    @Query("""
            SELECT p FROM Profile p
             WHERE p.status = com.siide.linkup.feature.profile.domain.model.ProfileStatus.DELETION_PENDING
               AND p.deletionScheduledAt <= :now
             ORDER BY p.deletionScheduledAt ASC
            """)
    List<Profile> findExpiredDeletionsPaged(@Param("now") Instant now, PageRequest page);

    @Override
    default List<Profile> findExpiredDeletions(Instant now, int limit) {
        return findExpiredDeletionsPaged(now, PageRequest.of(0, Math.max(1, limit)));
    }
}
