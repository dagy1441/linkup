package com.siide.linkup.feature.profile.application.dto;

import java.util.Set;

/**
 * Replace the user's interests with the given set of catalogue slugs.
 * Empty set clears all picks.
 */
public record UpdateInterestsCommand(Set<String> slugs) {

    public UpdateInterestsCommand {
        if (slugs == null) slugs = Set.of();
    }
}
