package com.siide.linkup.feature.activity.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Activity-module configuration bound from {@code linkup.activity.*}.
 *
 * @param defaultPageSize default page size when the caller doesn't specify one
 * @param maxPageSize     hard upper bound on page size to prevent abuse
 */
@ConfigurationProperties(prefix = "linkup.activity")
public record ActivityProperties(int defaultPageSize, int maxPageSize) {

    public ActivityProperties {
        if (defaultPageSize <= 0) defaultPageSize = 20;
        if (maxPageSize <= 0) maxPageSize = 100;
        if (defaultPageSize > maxPageSize) {
            throw new IllegalArgumentException(
                    "linkup.activity.default-page-size (" + defaultPageSize +
                            ") cannot exceed max-page-size (" + maxPageSize + ")");
        }
    }
}
