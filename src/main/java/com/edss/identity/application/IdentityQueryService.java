package com.edss.identity.application;

import com.edss.identity.infrastructure.UserRepository;
import com.edss.identity.spi.IdentityQuery;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class IdentityQueryService implements IdentityQuery {

    private final UserRepository users;

    IdentityQueryService(UserRepository users) {
        this.users = users;
    }

    @Override
    public Optional<UserSummary> findUser(UUID userId) {
        return users.findById(userId).map(IdentityQueryService::toSummary);
    }

    @Override
    public Optional<UserSummary> findUserByEmail(String email) {
        return users.findByEmailIgnoreCase(email).map(IdentityQueryService::toSummary);
    }

    private static UserSummary toSummary(com.edss.identity.domain.User u) {
        return new UserSummary(
                u.getId(),
                u.getEmail(),
                u.getName(),
                u.getAvatarUrl(),
                u.getPrimaryRole(),
                u.isHasBothRoles(),
                u.getCreatedAt());
    }
}
