package com.edss.identity.infrastructure;

import com.edss.identity.domain.UserTwoFactor;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserTwoFactorRepository extends JpaRepository<UserTwoFactor, UUID> {

    Optional<UserTwoFactor> findByUserId(UUID userId);
}
