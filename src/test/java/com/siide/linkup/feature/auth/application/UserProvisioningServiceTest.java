package com.siide.linkup.feature.auth.application;

import com.siide.linkup.feature.auth.domain.UserRepository;
import com.siide.linkup.feature.auth.domain.event.UserProvisionedEvent;
import com.siide.linkup.feature.auth.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserProvisioningServiceTest {

    private UserRepository repository;
    private ApplicationEventPublisher publisher;
    private UserProvisioningService service;

    @BeforeEach
    void setUp() {
        repository = mock(UserRepository.class);
        publisher = mock(ApplicationEventPublisher.class);
        service = new UserProvisioningService(repository, publisher);
        when(repository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void creates_user_and_publishes_event_when_keycloak_id_not_seen() {
        Jwt jwt = jwt("kc-1", "a@b.com", "Alice", List.of("user"));
        when(repository.findByKeycloakId("kc-1")).thenReturn(Optional.empty());

        User result = service.ensureLocalUser(jwt);

        assertThat(result.getKeycloakId()).isEqualTo("kc-1");
        assertThat(result.getEmail()).isEqualTo("a@b.com");
        assertThat(result.getRoles()).containsExactly("user");

        ArgumentCaptor<UserProvisionedEvent> eventCaptor = ArgumentCaptor.forClass(UserProvisionedEvent.class);
        verify(publisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().keycloakId()).isEqualTo("kc-1");
    }

    @Test
    void refreshes_existing_user_without_publishing_event() {
        User existing = User.provision("kc-1", "old@b.com", "Old", Set.of("user"));
        when(repository.findByKeycloakId("kc-1")).thenReturn(Optional.of(existing));

        Jwt jwt = jwt("kc-1", "new@b.com", "New Name", List.of("user", "organizer"));
        User result = service.ensureLocalUser(jwt);

        assertThat(result.getEmail()).isEqualTo("new@b.com");
        assertThat(result.getDisplayName()).isEqualTo("New Name");
        assertThat(result.getRoles()).containsExactlyInAnyOrder("user", "organizer");
        verify(publisher, never()).publishEvent(any());
    }

    @Test
    void fails_when_creating_user_without_email_claim() {
        Jwt jwt = jwt("kc-1", null, null, List.of());
        when(repository.findByKeycloakId("kc-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.ensureLocalUser(jwt))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("email claim missing");
    }

    @Test
    void falls_back_to_preferred_username_when_name_missing() {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "RS256")
                .subject("kc-1")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60))
                .claim("email", "a@b.com")
                .claim("preferred_username", "alice42")
                .build();
        when(repository.findByKeycloakId("kc-1")).thenReturn(Optional.empty());

        User result = service.ensureLocalUser(jwt);
        assertThat(result.getDisplayName()).isEqualTo("alice42");
    }

    private Jwt jwt(String sub, String email, String name, List<String> roles) {
        Jwt.Builder b = Jwt.withTokenValue("t").header("alg", "RS256")
                .subject(sub)
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60));
        if (email != null) b.claim("email", email);
        if (name != null) b.claim("name", name);
        if (roles != null) b.claim("realm_access", Map.of("roles", roles));
        return b.build();
    }
}
