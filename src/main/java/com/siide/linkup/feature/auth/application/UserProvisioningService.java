package com.siide.linkup.feature.auth.application;

import com.siide.linkup.feature.auth.domain.UserRepository;
import com.siide.linkup.feature.auth.domain.event.UserProvisionedEvent;
import com.siide.linkup.feature.auth.domain.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Idempotent use-case that materialises a Keycloak identity into the local {@code users}
 * table on first contact and refreshes identity attributes on subsequent requests.
 */
@Service
public class UserProvisioningService {

    private static final Logger log = LoggerFactory.getLogger(UserProvisioningService.class);

    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    public UserProvisioningService(UserRepository userRepository, ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public User ensureLocalUser(Jwt jwt) {
        String keycloakId = jwt.getSubject();
        String email = stringClaim(jwt, "email");
        String displayName = firstNonBlank(
                stringClaim(jwt, "name"),
                stringClaim(jwt, "preferred_username"),
                email
        );
        Set<String> roles = extractRealmRoles(jwt);

        return userRepository.findByKeycloakId(keycloakId)
                .map(existing -> refresh(existing, email, displayName, roles))
                .orElseGet(() -> create(keycloakId, email, displayName, roles));
    }

    private User create(String keycloakId, String email, String displayName, Set<String> roles) {
        if (email == null) {
            // Defensive: Keycloak should always provide email for provisioned users
            throw new IllegalStateException(
                    "Cannot provision user " + keycloakId + ": email claim missing in JWT");
        }
        User user = User.provision(keycloakId, email, displayName, roles);
        User saved = userRepository.save(user);
        log.info("Provisioned local user id={} keycloakId={}", saved.getId(), keycloakId);
        eventPublisher.publishEvent(UserProvisionedEvent.of(saved.getId(), keycloakId, email));
        return saved;
    }

    private User refresh(User existing, String email, String displayName, Set<String> roles) {
        if (email != null) {
            existing.syncFromIdentity(email, displayName, roles);
            return userRepository.save(existing);
        }
        return existing;
    }

    private static String stringClaim(Jwt jwt, String name) {
        Object value = jwt.getClaim(name);
        return value instanceof String s && !s.isBlank() ? s : null;
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Set<String> extractRealmRoles(Jwt jwt) {
        Object realmAccess = jwt.getClaim("realm_access");
        if (!(realmAccess instanceof Map<?, ?> map)) return Collections.emptySet();
        Object roles = map.get("roles");
        if (!(roles instanceof Collection<?> collection)) return Collections.emptySet();
        return collection.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }
}
