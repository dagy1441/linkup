package com.siide.linkup.feature.profile.infrastructure.persistence.jpa;

import com.siide.linkup.feature.profile.domain.InterestCatalog;
import com.siide.linkup.feature.profile.domain.model.Interest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public interface JpaInterestCatalog extends JpaRepository<Interest, String>, InterestCatalog {

    @Override
    @Query("""
            SELECT i FROM Interest i
             WHERE i.enabled = true
             ORDER BY i.sortOrder ASC, i.slug ASC
            """)
    List<Interest> findAllEnabled();

    @Override
    @Query("""
            SELECT i FROM Interest i
             WHERE i.enabled = true AND i.slug IN :slugs
             ORDER BY i.sortOrder ASC, i.slug ASC
            """)
    List<Interest> findEnabledBySlugs(@Param("slugs") Collection<String> slugs);

    @Override
    default Set<String> filterValidSlugs(Collection<String> slugs) {
        if (slugs == null || slugs.isEmpty()) return Set.of();
        return findEnabledBySlugs(slugs).stream()
                .map(Interest::getSlug)
                .collect(Collectors.toUnmodifiableSet());
    }
}
