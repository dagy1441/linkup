package com.siide.linkup.feature.auth.api;

import java.util.UUID;

/**
 * Minimal cross-module facade exposing the authenticated user's internal id. Modules
 * needing more than the id should still depend on this contract and add their own
 * lookup against their own bounded context.
 */
public interface CurrentUserAccessor {

    /** Internal {@code users.id} of the authenticated principal. Provisions on demand. */
    UUID requireCurrentUserId();
}
