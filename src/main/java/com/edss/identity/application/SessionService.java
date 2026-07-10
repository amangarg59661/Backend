package com.edss.identity.application;

import com.edss.identity.domain.Session;
import com.edss.identity.infrastructure.SessionRepository;
import com.edss.shared.api.ApiErrorCode;
import com.edss.shared.api.ApiException;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SessionService {

    private final SessionRepository sessions;
    private final Clock clock;

    public SessionService(SessionRepository sessions, Clock clock) {
        this.sessions = sessions;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<Session> listActive(UUID userId) {
        return sessions.findByUserIdAndRevokedAtIsNullOrderByLastActiveAtDesc(userId);
    }

    public void revoke(UUID userId, UUID sessionId) {
        Session session =
                sessions.findById(sessionId)
                        .filter(s -> s.getUserId().equals(userId))
                        .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND, "Session not found."));
        if (!session.isActive()) {
            return;
        }
        session.revoke(userId, clock.instant());
    }

    @Transactional(readOnly = true)
    public boolean isActive(UUID sessionId) {
        return sessions.findById(sessionId).map(Session::isActive).orElse(false);
    }
}
