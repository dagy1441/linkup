package com.siide.linkup.feature.activity.infrastructure.rest.controller;

import com.siide.linkup.feature.activity.application.ActivityCommandService;
import com.siide.linkup.feature.activity.application.ActivityCoverProperties;
import com.siide.linkup.feature.activity.application.ActivityQueryService;
import com.siide.linkup.feature.activity.application.dto.CreateActivityCommand;
import com.siide.linkup.feature.activity.application.dto.UpdateActivityCommand;
import com.siide.linkup.feature.activity.domain.exception.InvalidCoverException;
import com.siide.linkup.feature.activity.domain.model.Activity;
import com.siide.linkup.feature.activity.domain.storage.CoverStorageService;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
    private final CoverStorageService coverStorage;
    private final ActivityCoverProperties coverProperties;

    public ActivityController(ActivityCommandService commandService,
                              ActivityQueryService queryService,
                              CurrentUserAccessor currentUserAccessor,
                              UserDirectory userDirectory,
                              CoverStorageService coverStorage,
                              ActivityCoverProperties coverProperties) {
        this.commandService = commandService;
        this.queryService = queryService;
        this.currentUserAccessor = currentUserAccessor;
        this.userDirectory = userDirectory;
        this.coverStorage = coverStorage;
        this.coverProperties = coverProperties;
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
                a -> ActivityResponse.from(a,
                        organizerNames.getOrDefault(a.getOrganizerId(), UNKNOWN_ORGANIZER),
                        coverUrlOf(a))));
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
        return ResponseEntity.ok(toResponse(activity));
    }

    @PostMapping
    @PreAuthorize("hasRole('ORGANIZER')")
    @Operation(summary = "Create a new activity (ORGANIZER role required).")
    public ResponseEntity<ActivityResponse> create(@Valid @RequestBody ActivityRequest body) {
        UUID organizerId = currentUserAccessor.requireCurrentUserId();
        CreateActivityCommand cmd = new CreateActivityCommand(
                body.title(), body.description(), body.category(), body.city(), body.addressLine(),
                body.latitude(), body.longitude(), body.startsAt(), body.capacity());
        Activity created = commandService.create(cmd, organizerId);
        log.info("POST /activities -> created id={}", created.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZER')")
    @Operation(summary = "Update an activity (ORGANIZER role + activity owner only).")
    public ResponseEntity<ActivityResponse> update(@PathVariable UUID id,
                                                   @Valid @RequestBody ActivityRequest body) {
        UUID userId = currentUserAccessor.requireCurrentUserId();
        UpdateActivityCommand cmd = new UpdateActivityCommand(
                body.title(), body.description(), body.category(), body.city(), body.addressLine(),
                body.latitude(), body.longitude(), body.startsAt(), body.capacity());
        Activity updated = commandService.update(id, cmd, userId);
        return ResponseEntity.ok(toResponse(updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZER')")
    @Operation(summary = "Cancel an activity (ORGANIZER role + activity owner only). 422 if already cancelled.")
    public ResponseEntity<ActivityResponse> cancel(@PathVariable UUID id) {
        UUID userId = currentUserAccessor.requireCurrentUserId();
        Activity cancelled = commandService.cancel(id, userId);
        return ResponseEntity.ok(toResponse(cancelled));
    }

    @PostMapping(value = "/{id}/cover", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ORGANIZER')")
    @Operation(summary = "Upload (or replace) the activity cover image. Max 2 MB, JPEG / PNG / WebP only.")
    public ResponseEntity<ActivityResponse> uploadCover(@PathVariable UUID id,
                                                       @RequestParam("file") MultipartFile file) throws IOException {
        UUID userId = currentUserAccessor.requireCurrentUserId();
        if (file == null || file.isEmpty()) {
            throw new InvalidCoverException("file part is missing or empty");
        }
        Activity updated = commandService.uploadCover(id, userId,
                file.getContentType(), file.getSize(), file.getInputStream());
        return ResponseEntity.ok(toResponse(updated));
    }

    @DeleteMapping("/{id}/cover")
    @PreAuthorize("hasRole('ORGANIZER')")
    @Operation(summary = "Remove the activity cover image (idempotent).")
    public ResponseEntity<ActivityResponse> deleteCover(@PathVariable UUID id) {
        UUID userId = currentUserAccessor.requireCurrentUserId();
        Activity updated = commandService.removeCover(id, userId);
        return ResponseEntity.ok(toResponse(updated));
    }

    private ActivityResponse toResponse(Activity activity) {
        return ActivityResponse.from(activity, resolveOrganizerName(activity), coverUrlOf(activity));
    }

    private String coverUrlOf(Activity a) {
        return coverStorage.presignedUrl(a.getCoverKey(), coverProperties.presignedUrlTtl());
    }

    private String resolveOrganizerName(Activity activity) {
        return userDirectory.findDisplayName(activity.getOrganizerId()).orElse(UNKNOWN_ORGANIZER);
    }
}
