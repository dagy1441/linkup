package com.siide.linkup.feature.profile.infrastructure.rest.dto;

import com.siide.linkup.feature.profile.domain.model.Interest;
import com.siide.linkup.feature.profile.domain.model.InterestCategory;

public record InterestResponse(
        String slug,
        String labelFr,
        String labelEn,
        InterestCategory category,
        String icon,
        int sortOrder
) {
    public static InterestResponse from(Interest i) {
        return new InterestResponse(
                i.getSlug(),
                i.getLabelFr(),
                i.getLabelEn(),
                i.getCategory(),
                i.getIcon(),
                i.getSortOrder()
        );
    }
}
