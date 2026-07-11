package com.edss.integrations.calendar;

/**
 * Verifies + parses booking webhooks from a calendar provider. Each impl
 * knows its own signature header + payload shape. The parsed result is
 * provider-neutral so {@link com.edss.integrations.calendar.CalendarWebhookService}
 * can apply it uniformly.
 */
public interface CalendarWebhookClient {

    String providerId();

    Result verify(String signatureHeader, String rawBody);

    record Result(
            boolean valid,
            String externalEventId,
            String eventType,
            java.util.UUID projectId,
            java.time.Instant scheduledAt,
            String meetingUrl,
            String reason) {

        public static Result invalid(String reason) {
            return new Result(false, null, null, null, null, null, reason);
        }
    }
}
