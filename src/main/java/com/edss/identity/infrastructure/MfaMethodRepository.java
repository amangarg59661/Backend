package com.edss.identity.infrastructure;

import com.edss.identity.domain.MfaMethod;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MfaMethodRepository extends JpaRepository<MfaMethod, UUID> {

    Optional<MfaMethod> findByUserIdAndMethodType(UUID userId, String methodType);

    List<MfaMethod> findByUserIdAndEnabledTrue(UUID userId);

    List<MfaMethod> findByUserId(UUID userId);
}
