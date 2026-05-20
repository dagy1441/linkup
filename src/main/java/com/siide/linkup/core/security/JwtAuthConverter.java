package com.siide.linkup.core.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Converts a Keycloak {@link Jwt} into a {@link JwtAuthenticationToken} where each role
 * declared at {@link JwtProperties#roleClaim()} becomes a {@code ROLE_<NAME>} authority.
 * The {@code sub} claim is used as the principal name so downstream code can rely on a
 * stable Keycloak user id.
 */
@Component
public class JwtAuthConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final String ROLE_PREFIX = "ROLE_";

    private final JwtProperties properties;

    public JwtAuthConverter(JwtProperties properties) {
        this.properties = properties;
    }

    @Override
    public JwtAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = extractRoles(jwt).stream()
                .map(role -> new SimpleGrantedAuthority(ROLE_PREFIX + role.toUpperCase()))
                .collect(Collectors.toUnmodifiableSet());
        return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
    }

    @SuppressWarnings("unchecked")
    private Set<String> extractRoles(Jwt jwt) {
        String[] path = properties.roleClaim().split("\\.");
        Object current = jwt.getClaims();
        for (String segment : path) {
            if (!(current instanceof Map<?, ?> map)) {
                return Collections.emptySet();
            }
            current = map.get(segment);
            if (current == null) {
                return Collections.emptySet();
            }
        }
        if (current instanceof Collection<?> collection) {
            return collection.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .filter(s -> !s.isBlank())
                    .collect(Collectors.toUnmodifiableSet());
        }
        if (current instanceof String single && !single.isBlank()) {
            return Set.copyOf(List.of(single));
        }
        return Stream.<String>empty().collect(Collectors.toUnmodifiableSet());
    }
}
