package com.siide.linkup.feature.activity.application.dto;

import com.siide.linkup.feature.activity.domain.model.ActivityCategory;

import java.time.Instant;

public record CreateActivityCommand(
        String title,
        String description,
        ActivityCategory category,
        String city,
        String addressLine,
        Double latitude,
        Double longitude,
        Instant startsAt,
        int capacity
) {}
