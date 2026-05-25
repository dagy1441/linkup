package com.siide.linkup.feature.profile.domain.model;

/**
 * Self-declared gender. Inclusive set; the UI surfaces the option to skip
 * the field entirely (stored as {@code null}). {@link #UNDISCLOSED} is the
 * explicit "prefer not to say" choice — distinct from "not yet answered".
 */
public enum Gender {
    MALE,
    FEMALE,
    OTHER,
    UNDISCLOSED
}
