package com.siide.linkup.core.audit;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Resolves the principal performing the current request for JPA auditing.
 * Falls back to {@code "system"} when no authentication is present (e.g. background jobs).
 */
@Component
public class AuditorAwareImpl implements AuditorAware<String> {

    private static final String SYSTEM = "system";

    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return Optional.of(SYSTEM);
        }
        if (auth.getPrincipal() instanceof Jwt jwt) {
            return Optional.ofNullable(jwt.getSubject()).or(() -> Optional.of(SYSTEM));
        }
        return Optional.of(auth.getName());
    }
}
