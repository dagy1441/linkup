package com.siide.linkup.feature.profile.application;

import com.siide.linkup.feature.profile.domain.ProfileRepository;
import com.siide.linkup.feature.profile.domain.model.Profile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * Daily sweep that hard-purges profiles whose deletion grace period has elapsed
 * (US-012). Each profile is purged in its own transaction so a partial failure
 * doesn't block the rest of the batch.
 * <p>
 * Default cron: 04:00 UTC (after the idempotency sweep at 03:00 — non-overlapping).
 */
@Component
public class ProfileDeletionScheduler {

    private static final Logger log = LoggerFactory.getLogger(ProfileDeletionScheduler.class);
    private static final int BATCH_LIMIT = 500;

    private final ProfileRepository repository;
    private final ProfileCommandService commandService;
    private final Clock clock;

    public ProfileDeletionScheduler(ProfileRepository repository,
                                    ProfileCommandService commandService,
                                    Clock clock) {
        this.repository = repository;
        this.commandService = commandService;
        this.clock = clock;
    }

    @Scheduled(cron = "${linkup.profile.lifecycle.purge-cron:0 0 4 * * *}")
    public void sweep() {
        Instant now = Instant.now(clock);
        List<Profile> due = repository.findExpiredDeletions(now, BATCH_LIMIT);
        if (due.isEmpty()) {
            log.debug("Profile purge sweep: nothing to do at {}", now);
            return;
        }
        log.info("Profile purge sweep: {} profile(s) due", due.size());
        int purged = 0;
        int errored = 0;
        for (Profile profile : due) {
            try {
                commandService.purge(profile);
                purged++;
            } catch (RuntimeException e) {
                errored++;
                log.error("Purge failed for profile id={} — will retry next sweep",
                        profile.getId(), e);
            }
        }
        log.info("Profile purge sweep done: purged={} errored={}", purged, errored);
    }
}
