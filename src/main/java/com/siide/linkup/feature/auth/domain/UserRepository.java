package com.siide.linkup.feature.auth.domain;

import com.siide.linkup.feature.auth.domain.model.User;

import java.util.Optional;
import java.util.UUID;

/**
 * Domain port for {@link User} persistence. Infrastructure provides the Spring Data implementation.
 * Methods are intentionally minimal — only what use-cases need.
 */
public interface UserRepository {

    Optional<User> findByKeycloakId(String keycloakId);

    Optional<User> findById(UUID id);

    boolean existsByKeycloakId(String keycloakId);

    User save(User user);
}
