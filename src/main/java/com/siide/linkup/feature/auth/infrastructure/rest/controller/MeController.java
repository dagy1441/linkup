package com.siide.linkup.feature.auth.infrastructure.rest.controller;

import com.siide.linkup.feature.auth.application.CurrentUserService;
import com.siide.linkup.feature.auth.domain.model.User;
import com.siide.linkup.feature.auth.infrastructure.rest.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "Authenticated user identity")
public class MeController {

    private static final Logger log = LoggerFactory.getLogger(MeController.class);

    private final CurrentUserService currentUserService;

    public MeController(CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
    }

    @GetMapping("/me")
    @Operation(summary = "Return the authenticated user's local profile (auto-provisioned).")
    public ResponseEntity<UserResponse> me() {
        User user = currentUserService.getCurrent();
        log.debug("GET /me userId={} keycloakId={}", user.getId(), user.getKeycloakId());
        return ResponseEntity.ok(UserResponse.from(user));
    }
}
