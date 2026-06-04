package com.siide.linkup.feature.activity.infrastructure.rest.controller;

import com.siide.linkup.feature.activity.application.ActivityCommandService;
import com.siide.linkup.feature.activity.application.ActivityQueryService;
import com.siide.linkup.feature.activity.application.dto.CreateActivityCommand;
import com.siide.linkup.feature.activity.application.dto.UpdateActivityCommand;
import com.siide.linkup.feature.activity.domain.model.Activity;
import com.siide.linkup.feature.activity.infrastructure.rest.dto.ActivityRequest;
import com.siide.linkup.feature.activity.infrastructure.rest.dto.ActivityResponse;
import com.siide.linkup.feature.auth.api.CurrentUserAccessor;
import com.siide.linkup.feature.auth.api.UserDirectory;
import com.siide.linkup.shared.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/activities")
@Tag(name = "Activities", description = "Public catalog of activities; ORGANIZER-restricted writes")
public class ActivityController {

    private static final Logger log = LoggerFactory.getLogger(ActivityController.class);
    private static final String UNKNOWN_ORGANIZER = "Unknown organizer";

    private final ActivityCommandService commandService;
    private final ActivityQueryService queryService;
    private final CurrentUserAccessor currentUserAccessor;
    private final UserDirectory userDirectory;

    public ActivityController(ActivityCommandService commandService,
                              ActivityQueryService queryService,
                              CurrentUserAccessor currentUserAccessor,
                              UserDirectory userDirectory) {
        this.commandService = commandService;
        this.queryService = queryService;
        this.currentUserAccessor = currentUserAccessor;
        this.userDirectory = userDirectory;
    }

    @GetMapping
    @Operation(summary = "List published, upcoming activities (paginated, optional city filter).")
    public ResponseEntity<PageResponse<ActivityResponse>> list(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        Page<Activity> result = queryService.listPublishedUpcoming(Optional.ofNullable(city), page, size);
        Map<UUID, String> organizerNames = userDirectory.findDisplayNames(
                result.getContent().stream().map(Activity::getOrganizerId).toList());
        return ResponseEntity.ok(PageResponse.from(result,
                a -> ActivityResponse.from(a, organizerNames.getOrDefault(a.getOrganizerId(), UNKNOWN_ORGANIZER))));
    }

    @GetMapping("/mine")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List activities organized by the authenticated user (any status, sorted by date DESC).")
    public ResponseEntity<PageResponse<ActivityResponse>> listMine(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        UUID organizerId = currentUserAccessor.requireCurrentUserId();
        Page<Activity> result = queryService.listMine(organizerId, page, size);
        String name = userDirectory.findDisplayName(organizerId).orElse(UNKNOWN_ORGANIZER);
        return ResponseEntity.ok(PageResponse.from(result, a -> ActivityResponse.from(a, name)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single activity by id.")
    public ResponseEntity<ActivityResponse> getById(@PathVariable UUID id) {
        Activity activity = queryService.getById(id);
        return ResponseEntity.ok(ActivityResponse.from(activity, resolveOrganizerName(activity)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ORGANIZER')")
    @Operation(summary = "Create a new activity (ORGANIZER role required).")
    public ResponseEntity<ActivityResponse> create(@Valid @RequestBody ActivityRequest body) {
        UUID organizerId = currentUserAccessor.requireCurrentUserId();
        CreateActivityCommand cmd = new CreateActivityCommand(
                body.title(), body.description(), body.city(), body.addressLine(),
                body.latitude(), body.longitude(), body.startsAt(), body.capacity());
        Activity created = commandService.create(cmd, organizerId);
        log.info("POST /activities → created id={}", created.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ActivityResponse.from(created, resolveOrganizerName(created)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZER')")
    @Operation(summary = "Update an activity (ORGANIZER role + activity owner only).")
    public ResponseEntity<ActivityResponse> update(@PathVariable UUID id,
                                                   @Valid @RequestBody ActivityRequest body) {
        UUID userId = currentUserAccessor.requireCurrentUserId();
        UpdateActivityCommand cmd = new UpdateActivityCommand(
                body.title(), body.description(), body.city(), body.addressLine(),
                body.latitude(), body.longitude(), body.startsAt(), body.capacity());
        Activity updated = commandService.update(id, cmd, userId);
        return ResponseEntity.ok(ActivityResponse.from(updated, resolveOrganizerName(updated)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZER')")
    @Operation(summary = "Cancel an activity (ORGANIZER role + activity owner only). 422 if already cancelled.")
    public ResponseEntity<ActivityResponse> cancel(@PathVariable UUID id) {
        UUID userId = currentUserAccessor.requireCurrentUserId();
        Activity cancelled = commandService.cancel(id, userId);
        return ResponseEntity.ok(ActivityResponse.from(cancelled, resolveOrganizerName(cancelled)));
    }

    private String resolveOrganizerName(Activity activity) {
        return userDirectory.findDisplayName(activity.getOrganizerId()).orElse(UNKNOWN_ORGANIZER);
    }
}
