package com.siide.linkup.core.security;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CorsPropertiesTest {

    @Test
    void empty_allowed_origins_falls_back_to_localhost_defaults() {
        CorsProperties props = new CorsProperties(null, null, null, null, true, 0);

        assertThat(props.allowedOrigins())
                .containsExactly("http://localhost:*", "http://127.0.0.1:*");
        assertThat(props.allowedMethods())
                .contains("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
        assertThat(props.allowedHeaders()).containsExactly("*");
        assertThat(props.exposedHeaders()).contains("Location", "Idempotency-Key");
        assertThat(props.allowCredentials()).isTrue();
        assertThat(props.maxAge()).isEqualTo(3600L);
    }

    @Test
    void production_origins_override_dev_defaults() {
        CorsProperties props = new CorsProperties(
                List.of("https://app.linkup.io", "https://m.linkup.io"),
                List.of("GET", "POST"),
                List.of("Authorization", "Content-Type"),
                List.of("Location"),
                false,
                7200);

        assertThat(props.allowedOrigins())
                .containsExactly("https://app.linkup.io", "https://m.linkup.io");
        assertThat(props.allowedMethods()).containsExactly("GET", "POST");
        assertThat(props.allowedHeaders()).containsExactly("Authorization", "Content-Type");
        assertThat(props.exposedHeaders()).containsExactly("Location");
        assertThat(props.allowCredentials()).isFalse();
        assertThat(props.maxAge()).isEqualTo(7200L);
    }

    @Test
    void exposed_headers_can_be_empty_but_not_null() {
        CorsProperties props = new CorsProperties(null, null, null, List.of(), true, 0);
        assertThat(props.exposedHeaders()).isEmpty();
    }
}
