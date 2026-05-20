package com.siide.linkup.feature.auth.infrastructure.rest.dto;

import com.siide.linkup.feature.auth.domain.model.User;
import com.siide.linkup.feature.auth.domain.model.UserStatus;

import java.util.Set;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String keycloakId,
        String email,
        String displayName,
        UserStatus status,
        Set<String> roles
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getKeycloakId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getStatus(),
                user.getRoles()
        );
    }
}
