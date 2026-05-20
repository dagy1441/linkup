package com.siide.linkup.core.idempotency;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, UUID> {

    Optional<IdempotencyKey> findByKeyAndUserIdAndEndpoint(String key, UUID userId, String endpoint);

    @Modifying
    @Query("DELETE FROM IdempotencyKey i WHERE i.expiresAt < :cutoff")
    int deleteExpired(@Param("cutoff") Instant cutoff);
}
