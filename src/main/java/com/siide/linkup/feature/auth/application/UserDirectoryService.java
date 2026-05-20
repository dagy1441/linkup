package com.siide.linkup.feature.auth.application;

import com.siide.linkup.feature.auth.api.UserDirectory;
import com.siide.linkup.feature.auth.domain.UserRepository;
import com.siide.linkup.feature.auth.domain.model.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class UserDirectoryService implements UserDirectory {

    private final UserRepository userRepository;

    public UserDirectoryService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Optional<String> findDisplayName(UUID userId) {
        return userRepository.findById(userId).map(User::getDisplayName);
    }

    @Override
    public Map<UUID, String> findDisplayNames(Collection<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userIds.stream()
                .distinct()
                .map(userRepository::findById)
                .flatMap(Optional::stream)
                .filter(u -> u.getDisplayName() != null)
                .collect(Collectors.toUnmodifiableMap(User::getId, User::getDisplayName));
    }
}
