package com.siide.linkup.feature.activity.domain.model;

/**
 * Top-level category of an activity. Used by participants to filter the feed,
 * and later matched against {@code profile.interests} for recommendations
 * (V2 — recommendation module).
 *
 * <p>The enum is intentionally closed: adding a value is a deliberate product
 * decision that requires a Flyway migration to update the
 * {@code ck_activities_category} check constraint.
 */
public enum ActivityCategory {
    CULTURE,
    FORMATION,
    SOIREE,
    TOURISME,
    SPORT,
    FESTIVAL,
    SCIENCE,
    GASTRONOMIE,
    BUSINESS
}
