package com.siide.linkup.feature.activity.application.dto;

import java.time.Instant;

public record UpdateActivityCommand(
        String title,
        String description,
        String city,
        String addressLine,
        Double latitude,
        Double longitude,
        Instant startsAt,
        int capacity
) {}
