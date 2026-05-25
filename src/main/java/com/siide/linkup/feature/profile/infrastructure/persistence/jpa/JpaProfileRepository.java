package com.siide.linkup.feature.profile.infrastructure.persistence.jpa;

import com.siide.linkup.feature.profile.domain.ProfileRepository;
import com.siide.linkup.feature.profile.domain.model.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface JpaProfileRepository extends JpaRepository<Profile, UUID>, ProfileRepository {
}
