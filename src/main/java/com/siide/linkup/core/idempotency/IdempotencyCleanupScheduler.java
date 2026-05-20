package com.siide.linkup.core.idempotency;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/**
 * Periodic sweep that removes expired idempotency rows. The {@code expires_at} index
 * keeps the DELETE cheap even when the table is large.
 */
@Component
public class IdempotencyCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyCleanupScheduler.class);

    private final IdempotencyKeyRepository repository;
    private final Clock clock;

    public IdempotencyCleanupScheduler(IdempotencyKeyRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Scheduled(cron = "${linkup.idempotency.cleanup-cron:0 0 3 * * *}")
    @Transactional
    public void sweep() {
        Instant cutoff = Instant.now(clock);
        int removed = repository.deleteExpired(cutoff);
        if (removed > 0) {
            log.info("Idempotency sweep removed {} expired key(s)", removed);
        }
    }
}
