package com.edss.integrations.calendar;

import com.edss.integrations.calendar.domain.CalendarWebhookEvent;
import com.edss.integrations.calendar.infrastructure.CalendarWebhookEventRepository;
import com.edss.projects.application.OnboardingCallService;
import com.edss.shared.api.ApiErrorCode;
import com.edss.shared.api.ApiException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Applies calendar webhooks idempotently. Records raw payload under
 * {@code (provider, external_event_id) UNIQUE} — duplicate deliveries become
 * no-ops. On a {@code booking created} event the project's
 * {@link com.edss.projects.domain.OnboardingCall} row is marked scheduled.
 */
@Service
public class CalendarWebhookService {

    private static final Logger log = LoggerFactory.getLogger(CalendarWebhookService.class);

    private final CalendarWebhookEventRepository events;
    private final OnboardingCallService onboardingCalls;
    private final Map<String, CalendarWebhookClient> clients;
    private final Clock clock;

    public CalendarWebhookService(
            CalendarWebhookEventRepository events,
            OnboardingCallService onboardingCalls,
            List<CalendarWebhookClient> clientList,
            Clock clock) {
        this.events = events;
        this.onboardingCalls = onboardingCalls;
        this.clients = new java.util.HashMap<>();
        for (CalendarWebhookClient c : clientList) {
            this.clients.put(c.providerId(), c);
        }
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(String provider, String signatureHeader, String rawBody) {
        CalendarWebhookClient client = clients.get(provider);
        if (client == null) {
            throw new ApiException(ApiErrorCode.NOT_FOUND, "Calendar provider not enabled: " + provider);
        }
        CalendarWebhookClient.Result verified = client.verify(signatureHeader, rawBody);
        if (!verified.valid()) {
            log.warn("Rejected {} calendar webhook: {}", provider, verified.reason());
            throw new ApiException(ApiErrorCode.FORBIDDEN, "Invalid webhook signature.");
        }

        Optional<CalendarWebhookEvent> existing =
                events.findByProviderAndExternalEventId(provider, verified.externalEventId());
        if (existing.isPresent() && "applied".equals(existing.get().getStatus())) {
            return;
        }

        Instant now = clock.instant();
        CalendarWebhookEvent record =
                new CalendarWebhookEvent(
                        UUID.randomUUID(),
                        provider,
                        verified.externalEventId(),
                        verified.projectId(),
                        rawBody,
                        now);
        try {
            events.save(record);
        } catch (DataIntegrityViolationException ex) {
            return;
        }

        try {
            if (isBookingEvent(verified.eventType()) && verified.projectId() != null && verified.scheduledAt() != null) {
                onboardingCalls.createOrUpdate(
                        verified.projectId(),
                        provider,
                        verified.scheduledAt(),
                        verified.meetingUrl(),
                        verified.externalEventId());
            }
            record.markApplied(clock.instant());
        } catch (RuntimeException ex) {
            record.markDead(clock.instant(), ex.getMessage());
            log.error("Failed to apply {} calendar webhook {}", provider, verified.externalEventId(), ex);
            throw ex;
        }
    }

    private static boolean isBookingEvent(String eventType) {
        if (eventType == null) return false;
        String lower = eventType.toLowerCase();
        return lower.contains("booking.created")
                || lower.contains("booking_created")
                || lower.contains("invitee.created");
    }
}
