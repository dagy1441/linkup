package com.siide.linkup.feature.auth.application;

import com.siide.linkup.feature.auth.api.CurrentUserAccessor;
import com.siide.linkup.feature.auth.domain.UserRepository;
import com.siide.linkup.feature.auth.domain.exception.UserNotFoundException;
import com.siide.linkup.feature.auth.domain.model.User;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves the authenticated principal to the local {@link User} aggregate. This is the
 * single entry point other modules use when they need the current user's internal UUID.
 */
@Service
public class CurrentUserService implements CurrentUserAccessor {

    private static final Logger log = LoggerFactory.getLogger(CurrentUserService.class);

    private final UserRepository userRepository;
    private final UserProvisioningService provisioningService;

    public CurrentUserService(UserRepository userRepository, UserProvisioningService provisioningService) {
        this.userRepository = userRepository;
        this.provisioningService = provisioningService;
    }

    @Transactional
    public User getCurrent() {
        Jwt jwt = currentJwt();
        return userRepository.findByKeycloakId(jwt.getSubject())
                .orElseGet(() -> {
                    log.info("Lazy-provisioning local user for keycloakId={}", jwt.getSubject());
                    return provisioningService.ensureLocalUser(jwt);
                });
    }

    public User requireById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("No user with id=" + userId));
    }

    @Override
    @Transactional
    public UUID requireCurrentUserId() {
        return getCurrent().getId();
    }

    private static Jwt currentJwt() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Jwt jwt)) {
            throw new IllegalStateException("No authenticated JWT in SecurityContext");
        }
        return jwt;
    }
}
