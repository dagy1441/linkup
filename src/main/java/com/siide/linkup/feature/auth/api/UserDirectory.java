package com.siide.linkup.feature.auth.api;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Read-only directory exposing user identity fields to other modules.
 * <p>
 * Provides only what is safe to share publicly (display names) so feature modules can
 * enrich responses without leaking the {@code User} aggregate or sensitive attributes.
 */
public interface UserDirectory {

    /** Resolve the display name for a single user. Empty when the user does not exist. */
    Optional<String> findDisplayName(UUID userId);

    /** Batch variant — returns a map keyed by user id; missing users are absent from the map. */
    Map<UUID, String> findDisplayNames(Collection<UUID> userIds);
}
