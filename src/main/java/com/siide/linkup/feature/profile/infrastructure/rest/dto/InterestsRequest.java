package com.siide.linkup.feature.profile.infrastructure.rest.dto;

import com.siide.linkup.feature.profile.domain.model.Profile;
import jakarta.validation.constraints.Size;

import java.util.Set;

/**
 * Replace the authenticated user's interests with the given catalogue slugs.
 * Empty / null slugs set clears all picks.
 */
public record InterestsRequest(
        @Size(max = Profile.MAX_INTERESTS) Set<String> slugs
) {}
