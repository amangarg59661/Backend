package com.edss.projects.application;

import com.edss.projects.domain.OnboardingCall;
import com.edss.projects.domain.events.ProjectEvents;
import com.edss.projects.infrastructure.OnboardingCallRepository;
import com.edss.shared.api.ApiErrorCode;
import com.edss.shared.api.ApiException;
import com.edss.shared.events.OutboxWriter;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OnboardingCallService implements com.edss.projects.spi.OnboardingCallScheduler {

    private static final java.util.Set<String> ALLOWED_PROVIDERS =
            java.util.Set.of("calcom", "calendly", "manual");

    private final OnboardingCallRepository calls;
    private final OutboxWriter outbox;
    private final Clock clock;

    public OnboardingCallService(
            OnboardingCallRepository calls, OutboxWriter outbox, Clock clock) {
        this.calls = calls;
        this.outbox = outbox;
        this.clock = clock;
    }

    @Override
    public void schedule(
            UUID projectId,
            String provider,
            Instant scheduledAt,
            String meetingUrl,
            String externalRef) {
        createOrUpdate(projectId, provider, scheduledAt, meetingUrl, externalRef);
    }

    public OnboardingCall createOrUpdate(
            UUID projectId,
            String provider,
            Instant scheduledAt,
            String meetingUrl,
            String externalRef) {
        if (!ALLOWED_PROVIDERS.contains(provider)) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_FAILED, "Provider must be calcom, calendly or manual.");
        }
        Instant now = clock.instant();
        OnboardingCall call =
                calls.findByProjectId(projectId)
                        .orElseGet(
                                () ->
                                        calls.save(
                                                new OnboardingCall(
                                                        UUID.randomUUID(),
                                                        projectId,
                                                        provider,
                                                        now)));
        if (scheduledAt != null) {
            call.markScheduled(scheduledAt, meetingUrl, externalRef, now);
            outbox.append(
                    "projects",
                    new ProjectEvents.OnboardingScheduled(
                            UUID.randomUUID(), now, projectId, provider, scheduledAt),
                    Map.of(
                            "project_id", projectId,
                            "provider", provider,
                            "scheduled_at", scheduledAt.toString()));
        }
        return call;
    }

    @Transactional(readOnly = true)
    public java.util.Optional<OnboardingCall> forProject(UUID projectId) {
        return calls.findByProjectId(projectId);
    }
}
