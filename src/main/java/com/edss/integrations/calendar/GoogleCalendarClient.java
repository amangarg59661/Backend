package com.edss.integrations.calendar;

import com.edss.shared.config.CalendarProperties;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Per-staff Google Calendar sync. Real OAuth flow: staff hits
 * {@code /integrations/google/authorize} → redirect to Google consent →
 * callback stores encrypted refresh_token in
 * {@code integrations.google_calendar_tokens}. This class holds the shape
 * of the token exchange + event insert; the OAuth callback route + full
 * event-insert HTTP call land alongside the frontend "Connect Google" UI.
 */
@Component
@ConditionalOnProperty(
        name = "edss.features.integrations.calendar.google-sync",
        havingValue = "true")
public class GoogleCalendarClient {

    private static final Logger log = LoggerFactory.getLogger(GoogleCalendarClient.class);

    private final CalendarProperties.Google config;

    public GoogleCalendarClient(CalendarProperties properties) {
        this.config = properties.google();
        if (config == null
                || config.oauthClientId() == null
                || config.oauthClientId().isBlank()
                || config.oauthClientSecret() == null
                || config.oauthClientSecret().isBlank()) {
            throw new IllegalStateException(
                    "GOOGLE_OAUTH_CLIENT_ID and GOOGLE_OAUTH_CLIENT_SECRET must be set when"
                            + " google-sync=true.");
        }
    }

    public String buildAuthorizeUrl(UUID staffUserId, String state) {
        return "https://accounts.google.com/o/oauth2/v2/auth?client_id="
                + config.oauthClientId()
                + "&redirect_uri=" + urlEncode(config.redirectUri())
                + "&response_type=code&access_type=offline&prompt=consent"
                + "&scope=" + urlEncode("https://www.googleapis.com/auth/calendar.events")
                + "&state=" + urlEncode(state);
    }

    /** Stub for now: real impl exchanges the code + persists tokens under {@code integrations.google_calendar_tokens}. */
    public void insertOnboardingEvent(UUID staffUserId, String summary, Instant scheduledAt, String meetingUrl) {
        log.info(
                "Google Calendar insert (stub) — staff={}, summary={}, at={}, url={}",
                staffUserId,
                summary,
                scheduledAt,
                meetingUrl);
    }

    private static String urlEncode(String s) {
        return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8);
    }
}
