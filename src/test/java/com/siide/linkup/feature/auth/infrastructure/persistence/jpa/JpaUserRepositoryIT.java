package com.siide.linkup.feature.auth.infrastructure.persistence.jpa;

import com.siide.linkup.core.audit.AuditorAwareImpl;
import com.siide.linkup.core.configuration.JpaAuditingConfig;
import com.siide.linkup.feature.auth.domain.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.AuditorAware;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.DockerClientFactory;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaAuditingConfig.class, JpaUserRepositoryIT.TestAuditingConfig.class})
@ActiveProfiles("test")
@EnabledIf(value = "dockerAvailable", disabledReason = "Docker not available on this host")
class JpaUserRepositoryIT {

    @TestConfiguration
    static class TestAuditingConfig {
        @Bean
        AuditorAware<String> auditorAwareImpl() {
            return new AuditorAwareImpl();
        }
    }

    @Autowired
    private JpaUserRepository repository;

    static boolean dockerAvailable() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (Throwable t) {
            return false;
        }
    }

    @Test
    void persists_and_finds_user_by_keycloak_id() {
        User user = User.provision("kc-1", "alice@linkup.io", "Alice", Set.of("user"));
        repository.saveAndFlush(user);

        Optional<User> found = repository.findByKeycloakId("kc-1");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("alice@linkup.io");
        assertThat(found.get().getRoles()).containsExactly("user");
        assertThat(found.get().getCreatedAt()).isNotNull();
    }

    @Test
    void exists_by_keycloak_id_returns_true_only_when_user_present() {
        repository.saveAndFlush(User.provision("kc-2", "b@linkup.io", "Bob", Set.of()));

        assertThat(repository.existsByKeycloakId("kc-2")).isTrue();
        assertThat(repository.existsByKeycloakId("kc-unknown")).isFalse();
    }

    @Test
    void enforces_unique_keycloak_id() {
        repository.saveAndFlush(User.provision("kc-dup", "c@linkup.io", "C", Set.of()));

        User duplicate = User.provision("kc-dup", "d@linkup.io", "D", Set.of());
        assertThatThrownBy(() -> repository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
