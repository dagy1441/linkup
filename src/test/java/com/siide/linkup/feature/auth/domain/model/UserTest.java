package com.siide.linkup.feature.auth.domain.model;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    @Test
    void provision_creates_active_user_with_generated_id() {
        User user = User.provision("kc-1", "a@b.com", "Alice", Set.of("user"));

        assertThat(user.getId()).isNotNull();
        assertThat(user.getKeycloakId()).isEqualTo("kc-1");
        assertThat(user.getEmail()).isEqualTo("a@b.com");
        assertThat(user.getDisplayName()).isEqualTo("Alice");
        assertThat(user.getRoles()).containsExactly("user");
        assertThat(user.isActive()).isTrue();
    }

    @Test
    void provision_rejects_blank_keycloak_id() {
        assertThatThrownBy(() -> User.provision(" ", "a@b.com", "Alice", Set.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("keycloakId");
    }

    @Test
    void provision_rejects_blank_email() {
        assertThatThrownBy(() -> User.provision("kc-1", null, "Alice", Set.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("email");
    }

    @Test
    void sync_replaces_mutable_identity_fields() {
        User user = User.provision("kc-1", "old@b.com", "Old", Set.of("user"));
        user.syncFromIdentity("new@b.com", "New", Set.of("organizer", "user"));

        assertThat(user.getEmail()).isEqualTo("new@b.com");
        assertThat(user.getDisplayName()).isEqualTo("New");
        assertThat(user.getRoles()).containsExactlyInAnyOrder("organizer", "user");
    }

    @Test
    void disable_then_enable_toggles_status() {
        User user = User.provision("kc-1", "a@b.com", "Alice", Set.of());
        user.disable();
        assertThat(user.isActive()).isFalse();
        user.enable();
        assertThat(user.isActive()).isTrue();
    }

    @Test
    void roles_collection_is_defensive_copy() {
        User user = User.provision("kc-1", "a@b.com", "Alice", Set.of("user"));
        assertThatThrownBy(() -> user.getRoles().add("hacker"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
