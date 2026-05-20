package com.siide.linkup.core.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for JWT role extraction.
 *
 * @param roleClaim dot-path within the JWT claims pointing to the role collection
 *                  (default: {@code realm_access.roles}).
 */
@ConfigurationProperties(prefix = "linkup.security.jwt")
public record JwtProperties(String roleClaim) {

    public JwtProperties {
        if (roleClaim == null || roleClaim.isBlank()) {
            roleClaim = "realm_access.roles";
        }
    }
}
