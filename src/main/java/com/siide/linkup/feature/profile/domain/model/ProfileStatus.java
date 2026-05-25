package com.siide.linkup.feature.profile.domain.model;

public enum ProfileStatus {
    /** Normal — user can interact with the platform. */
    ACTIVE,
    /**
     * Soft-deleted — the user requested account deletion. The profile is hidden
     * from the platform but kept for a grace period (default 30 days) so the
     * user can cancel. After the grace period the {@code ProfileDeletionScheduler}
     * hard-purges it (PR #8).
     */
    DELETION_PENDING
}
