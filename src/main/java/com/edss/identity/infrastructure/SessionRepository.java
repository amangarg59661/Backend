package com.edss.identity.infrastructure;

import com.edss.identity.domain.Session;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionRepository extends JpaRepository<Session, UUID> {

    List<Session> findByUserIdAndRevokedAtIsNullOrderByLastActiveAtDesc(UUID userId);
}
