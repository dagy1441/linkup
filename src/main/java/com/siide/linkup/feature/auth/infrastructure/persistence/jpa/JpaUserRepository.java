package com.siide.linkup.feature.auth.infrastructure.persistence.jpa;

import com.siide.linkup.feature.auth.domain.UserRepository;
import com.siide.linkup.feature.auth.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data adapter implementing the domain {@link UserRepository} port.
 * Method signatures intentionally match the port so Spring Data can generate them.
 */
@Repository
public interface JpaUserRepository extends JpaRepository<User, UUID>, UserRepository {

    @Override
    Optional<User> findByKeycloakId(String keycloakId);

    @Override
    boolean existsByKeycloakId(String keycloakId);
}
