package com.siide.linkup.feature.profile.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;

/**
 * Catalogue entry. Slug is the natural id (stable across renames) and what
 * profiles reference. Admin manages this table out-of-band today; an organizer
 * dashboard will land in V2.
 */
@Entity
@Table(name = "interests")
public class Interest {

    @Id
    @Column(name = "slug", nullable = false, updatable = false, length = 50)
    private String slug;

    @Column(name = "label_fr", nullable = false, length = 100)
    private String labelFr;

    @Column(name = "label_en", nullable = false, length = 100)
    private String labelEn;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private InterestCategory category;

    @Column(name = "icon", length = 50)
    private String icon;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected Interest() {
        // JPA
    }

    public String getSlug() { return slug; }
    public String getLabelFr() { return labelFr; }
    public String getLabelEn() { return labelEn; }
    public InterestCategory getCategory() { return category; }
    public String getIcon() { return icon; }
    public boolean isEnabled() { return enabled; }
    public int getSortOrder() { return sortOrder; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Interest other)) return false;
        return slug != null && slug.equals(other.slug);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(slug);
    }
}
