package com.siide.linkup.feature.profile.infrastructure.rest.controller;

import com.siide.linkup.feature.profile.domain.InterestCatalog;
import com.siide.linkup.feature.profile.infrastructure.rest.dto.InterestResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Public, read-only access to the interests catalogue. Used by the onboarding
 * flow (US-005) to render the chip picker — no auth required.
 */
@RestController
@RequestMapping("/api/v1/interests")
@Tag(name = "Interests", description = "Public catalogue of interests for the onboarding chip picker.")
public class InterestController {

    private final InterestCatalog catalog;

    public InterestController(InterestCatalog catalog) {
        this.catalog = catalog;
    }

    @GetMapping
    @Operation(summary = "List all enabled interests, sorted for UI consumption.")
    public ResponseEntity<List<InterestResponse>> list() {
        List<InterestResponse> body = catalog.findAllEnabled().stream()
                .map(InterestResponse::from)
                .toList();
        return ResponseEntity.ok(body);
    }
}
