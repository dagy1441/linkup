package com.siide.linkup.feature.activity.application;

import com.siide.linkup.feature.activity.domain.ActivityRepository;
import com.siide.linkup.feature.activity.domain.exception.ActivityNotFoundException;
import com.siide.linkup.feature.activity.domain.model.Activity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ActivityQueryService {

    private final ActivityRepository repository;
    private final ActivityProperties properties;
    private final Clock clock;

    public ActivityQueryService(ActivityRepository repository, ActivityProperties properties, Clock clock) {
        this.repository = repository;
        this.properties = properties;
        this.clock = clock;
    }

    public Activity getById(UUID id) {
        return repository.findById(id).orElseThrow(() -> new ActivityNotFoundException(id));
    }

    public Page<Activity> listPublishedUpcoming(Optional<String> city, Integer page, Integer size) {
        Pageable pageable = resolvePageable(page, size);
        Instant now = Instant.now(clock);
        return city.filter(c -> !c.isBlank())
                .map(c -> repository.findPublishedUpcomingByCity(c.trim().toLowerCase(), now, pageable))
                .orElseGet(() -> repository.findPublishedUpcoming(now, pageable));
    }

    /**
     * Activities organized by {@code organizerId}, regardless of status / date.
     * Powers the organizer dashboard — they need to see their cancelled and past
     * entries, not just the publicly listed slice. Sorted by {@code startsAt DESC}.
     */
    public Page<Activity> listMine(UUID organizerId, Integer page, Integer size) {
        int safePage = page == null || page < 0 ? 0 : page;
        int safeSize = size == null || size <= 0 ? properties.defaultPageSize() : size;
        safeSize = Math.min(safeSize, properties.maxPageSize());
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "startsAt"));
        return repository.findByOrganizerId(organizerId, pageable);
    }

    private Pageable resolvePageable(Integer page, Integer size) {
        int safePage = page == null || page < 0 ? 0 : page;
        int safeSize = size == null || size <= 0 ? properties.defaultPageSize() : size;
        safeSize = Math.min(safeSize, properties.maxPageSize());
        return PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.ASC, "startsAt"));
    }
}
