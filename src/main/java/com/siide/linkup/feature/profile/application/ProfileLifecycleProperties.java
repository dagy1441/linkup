package com.siide.linkup.feature.profile.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Settings driving the soft-delete / purge flow (US-012).
 *
 * @param deletionGracePeriod how long a {@code DELETION_PENDING} profile stays
 *                            recoverable before the scheduler hard-purges it.
 *                            Default 30 days — common GDPR-friendly window.
 * @param purgeCron           cron expression for the daily purge sweep.
 *                            Default 4am UTC (after the idempotency sweep at 3am).
 */
@ConfigurationProperties(prefix = "linkup.profile.lifecycle")
public record ProfileLifecycleProperties(
        Duration deletionGracePeriod,
        String purgeCron
) {
    public ProfileLifecycleProperties {
        if (deletionGracePeriod == null || deletionGracePeriod.isZero() || deletionGracePeriod.isNegative()) {
            deletionGracePeriod = Duration.ofDays(30);
        }
        if (purgeCron == null || purgeCron.isBlank()) {
            purgeCron = "0 0 4 * * *";
        }
    }
}
