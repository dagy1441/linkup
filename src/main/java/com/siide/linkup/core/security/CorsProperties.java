package com.siide.linkup.core.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * CORS configuration for the public HTTP API.
 *
 * @param allowedOrigins origin patterns allowed by the browser. Use full origins
 *                       ({@code https://app.linkup.io}) or wildcards
 *                       ({@code http://localhost:*}). Empty list disables CORS.
 * @param allowedMethods HTTP verbs accepted on cross-origin requests.
 * @param allowedHeaders request headers the browser may send. {@code *} = all.
 * @param exposedHeaders response headers the browser is allowed to read.
 * @param allowCredentials whether the browser may include cookies / auth headers.
 * @param maxAge cache lifetime (seconds) of the preflight response.
 */
@ConfigurationProperties(prefix = "linkup.security.cors")
public record CorsProperties(
        List<String> allowedOrigins,
        List<String> allowedMethods,
        List<String> allowedHeaders,
        List<String> exposedHeaders,
        boolean allowCredentials,
        long maxAge
) {

    public CorsProperties {
        if (allowedOrigins == null || allowedOrigins.isEmpty()) {
            // Dev default: any localhost port. Production MUST override via env vars.
            allowedOrigins = List.of("http://localhost:*", "http://127.0.0.1:*");
        }
        if (allowedMethods == null || allowedMethods.isEmpty()) {
            allowedMethods = List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
        }
        if (allowedHeaders == null || allowedHeaders.isEmpty()) {
            allowedHeaders = List.of("*");
        }
        if (exposedHeaders == null) {
            exposedHeaders = List.of("Location", "Idempotency-Key");
        }
        if (maxAge <= 0) {
            maxAge = 3600L;
        }
    }
}
