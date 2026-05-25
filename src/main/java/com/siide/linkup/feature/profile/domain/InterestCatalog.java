package com.siide.linkup.feature.profile.domain;

import com.siide.linkup.feature.profile.domain.model.Interest;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Read port for the curated interest catalogue.
 */
public interface InterestCatalog {

    /** All enabled interests, sorted by {@code sortOrder} then alphabetically. */
    List<Interest> findAllEnabled();

    /**
     * Resolve raw slugs to {@link Interest} rows that exist AND are enabled.
     * Unknown or disabled slugs are silently dropped — caller can compare sizes
     * to detect bad input.
     */
    List<Interest> findEnabledBySlugs(Collection<String> slugs);

    /** Convenience: returns only the valid+enabled slugs from the given set. */
    Set<String> filterValidSlugs(Collection<String> slugs);
}
