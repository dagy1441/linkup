package com.siide.linkup.feature.profile.infrastructure.rest.controller;

import com.siide.linkup.feature.auth.api.CurrentUserAccessor;
import com.siide.linkup.feature.profile.application.ProfileCommandService;
import com.siide.linkup.feature.profile.application.ProfileStorageProperties;
import com.siide.linkup.feature.profile.application.dto.UpdateInterestsCommand;
import com.siide.linkup.feature.profile.application.dto.UpdateProfileCommand;
import com.siide.linkup.feature.profile.domain.exception.InvalidPhotoException;
import com.siide.linkup.feature.profile.domain.model.Profile;
import com.siide.linkup.feature.profile.domain.storage.PhotoStorageService;
import com.siide.linkup.feature.profile.infrastructure.rest.dto.InterestsRequest;
import com.siide.linkup.feature.profile.infrastructure.rest.dto.ProfileRequest;
import com.siide.linkup.feature.profile.infrastructure.rest.dto.ProfileResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/profile")
@Tag(name = "Profile", description = "User profile: bio, city, DOB, gender, photo, interests, lifecycle.")
@PreAuthorize("isAuthenticated()")
public class ProfileController {

    private static final Logger log = LoggerFactory.getLogger(ProfileController.class);

    private final ProfileCommandService commandService;
    private final CurrentUserAccessor currentUserAccessor;
    private final PhotoStorageService photoStorage;
    private final ProfileStorageProperties storageProperties;

    public ProfileController(ProfileCommandService commandService,
                             CurrentUserAccessor currentUserAccessor,
                             PhotoStorageService photoStorage,
                             ProfileStorageProperties storageProperties) {
        this.commandService = commandService;
        this.currentUserAccessor = currentUserAccessor;
        this.photoStorage = photoStorage;
        this.storageProperties = storageProperties;
    }

    @GetMapping("/me")
    @Operation(summary = "Get the authenticated user's profile. Auto-provisions an empty profile on first call.")
    public ResponseEntity<ProfileResponse> me() {
        UUID userId = currentUserAccessor.requireCurrentUserId();
        Profile profile = commandService.ensureProfile(userId);
        return ResponseEntity.ok(toResponse(profile));
    }

    @PutMapping("/me")
    @Operation(summary = "Update the authenticated user's profile fields. Fires ProfileCompletedEvent on first completion.")
    public ResponseEntity<ProfileResponse> update(@Valid @RequestBody ProfileRequest body) {
        UUID userId = currentUserAccessor.requireCurrentUserId();
        Profile updated = commandService.update(userId,
                new UpdateProfileCommand(body.bio(), body.city(), body.dateOfBirth(), body.gender()));
        log.info("PUT /profile/me userId={} complete={}", userId, updated.isComplete());
        return ResponseEntity.ok(toResponse(updated));
    }

    @PutMapping("/me/interests")
    @Operation(summary = "Replace the authenticated user's interests with the given catalogue slugs. Unknown slugs are silently dropped.")
    public ResponseEntity<ProfileResponse> updateInterests(@Valid @RequestBody InterestsRequest body) {
        UUID userId = currentUserAccessor.requireCurrentUserId();
        Set<String> slugs = body.slugs() == null ? Set.of() : body.slugs();
        Profile updated = commandService.updateInterests(userId, new UpdateInterestsCommand(slugs));
        log.info("PUT /profile/me/interests userId={} count={} complete={}",
                userId, updated.getInterestSlugs().size(), updated.isComplete());
        return ResponseEntity.ok(toResponse(updated));
    }

    @PostMapping(value = "/me/photo", consumes = "multipart/form-data")
    @Operation(summary = "Upload (or replace) the authenticated user's profile photo. Max 1 MB, JPEG / PNG / WebP only.")
    public ResponseEntity<ProfileResponse> uploadPhoto(@RequestParam("file") MultipartFile file) throws IOException {
        UUID userId = currentUserAccessor.requireCurrentUserId();
        if (file == null || file.isEmpty()) {
            throw new InvalidPhotoException("file part is missing or empty");
        }
        Profile updated = commandService.uploadPhoto(userId,
                file.getContentType(), file.getSize(), file.getInputStream());
        return ResponseEntity.ok(toResponse(updated));
    }

    @DeleteMapping("/me/photo")
    @Operation(summary = "Remove the authenticated user's profile photo (idempotent).")
    public ResponseEntity<ProfileResponse> deletePhoto() {
        UUID userId = currentUserAccessor.requireCurrentUserId();
        Profile updated = commandService.removePhoto(userId);
        return ResponseEntity.ok(toResponse(updated));
    }

    /** Resolve the photo key to a time-limited presigned URL on the way out. */
    private ProfileResponse toResponse(Profile profile) {
        String url = photoStorage.presignedUrl(profile.getPhotoKey(),
                storageProperties.storage().presignedUrlTtl());
        return ProfileResponse.from(profile, url);
    }
}
