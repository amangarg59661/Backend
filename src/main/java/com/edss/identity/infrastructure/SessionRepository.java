package com.edss.identity.infrastructure;

import com.edss.identity.domain.Session;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionRepository extends JpaRepository<Session, UUID> {}
