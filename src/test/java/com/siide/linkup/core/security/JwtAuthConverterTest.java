package com.siide.linkup.core.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthConverterTest {

    private final JwtAuthConverter converter = new JwtAuthConverter(new JwtProperties("realm_access.roles"));

    @Test
    void maps_realm_roles_to_prefixed_authorities() {
        Jwt jwt = jwtBuilder()
                .claim("realm_access", Map.of("roles", List.of("organizer", "user")))
                .build();

        JwtAuthenticationToken token = converter.convert(jwt);

        assertThat(authoritiesOf(token)).containsExactlyInAnyOrder("ROLE_ORGANIZER", "ROLE_USER");
        assertThat(token.getName()).isEqualTo("user-123");
    }

    @Test
    void returns_empty_authorities_when_claim_missing() {
        Jwt jwt = jwtBuilder().build();

        JwtAuthenticationToken token = converter.convert(jwt);

        assertThat(token.getAuthorities()).isEmpty();
    }

    @Test
    void ignores_blank_role_values() {
        Jwt jwt = jwtBuilder()
                .claim("realm_access", Map.of("roles", List.of("user", "")))
                .build();

        assertThat(authoritiesOf(converter.convert(jwt))).containsExactly("ROLE_USER");
    }

    @Test
    void uses_custom_role_claim_path() {
        JwtAuthConverter custom = new JwtAuthConverter(new JwtProperties("resource_access.linkup.roles"));
        Jwt jwt = jwtBuilder()
                .claim("resource_access", Map.of("linkup", Map.of("roles", List.of("admin"))))
                .build();

        assertThat(authoritiesOf(custom.convert(jwt))).containsExactly("ROLE_ADMIN");
    }

    private Set<String> authoritiesOf(JwtAuthenticationToken token) {
        return token.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }

    private Jwt.Builder jwtBuilder() {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("user-123")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .claim("sub", "user-123");
    }
}
