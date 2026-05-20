package com.siide.linkup.feature.auth.domain.model;

import com.siide.linkup.core.audit.Auditable;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Local aggregate mirroring a Keycloak identity. Credentials remain owned by Keycloak;
 * this entity exists so other modules can reference users by a stable internal UUID.
 */
@Entity
@Table(
        name = "users",
        uniqueConstraints = @UniqueConstraint(name = "uk_users_keycloak_id", columnNames = "keycloak_id")
)
public class User extends Auditable {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "keycloak_id", nullable = false, length = 100)
    private String keycloakId;

    @Column(name = "email", nullable = false, length = 320)
    private String email;

    @Column(name = "display_name", length = 150)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id", nullable = false),
            uniqueConstraints = @UniqueConstraint(name = "uk_user_roles", columnNames = {"user_id", "role"})
    )
    @Column(name = "role", nullable = false, length = 50)
    private Set<String> roles = new HashSet<>();

    protected User() {
        // JPA
    }

    private User(UUID id, String keycloakId, String email, String displayName, Set<String> roles) {
        this.id = Objects.requireNonNull(id, "id");
        this.keycloakId = requireNonBlank(keycloakId, "keycloakId");
        this.email = requireNonBlank(email, "email");
        this.displayName = displayName;
        this.status = UserStatus.ACTIVE;
        this.roles = roles == null ? new HashSet<>() : new HashSet<>(roles);
    }

    public static User provision(String keycloakId, String email, String displayName, Set<String> roles) {
        return new User(UUID.randomUUID(), keycloakId, email, displayName, roles);
    }

    /** Refresh mutable identity attributes from the current JWT. Status is preserved. */
    public void syncFromIdentity(String email, String displayName, Set<String> roles) {
        this.email = requireNonBlank(email, "email");
        this.displayName = displayName;
        this.roles.clear();
        if (roles != null) this.roles.addAll(roles);
    }

    public void disable() { this.status = UserStatus.DISABLED; }

    public void enable() { this.status = UserStatus.ACTIVE; }

    public boolean isActive() { return status == UserStatus.ACTIVE; }

    public UUID getId() { return id; }
    public String getKeycloakId() { return keycloakId; }
    public String getEmail() { return email; }
    public String getDisplayName() { return displayName; }
    public UserStatus getStatus() { return status; }
    public Set<String> getRoles() { return Set.copyOf(roles); }

    private static String requireNonBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
