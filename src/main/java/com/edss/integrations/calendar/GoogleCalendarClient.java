package com.edss.integrations.calendar;

import com.edss.integrations.calendar.domain.GoogleCalendarToken;
import com.edss.integrations.calendar.infrastructure.GoogleCalendarTokenRepository;
import com.edss.shared.config.CalendarProperties;
import com.edss.shared.security.SecretCipher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Per-staff Google Calendar sync. Full OAuth exchange path:
 * <ol>
 *   <li>{@link #buildAuthorizeUrl(UUID, String)} → staff visits Google;</li>
 *   <li>Google redirects to {@code /integrations/google/callback?code=...};
 *       {@link #exchangeCode(UUID, String)} swaps the code for tokens;</li>
 *   <li>{@link #insertOnboardingEvent} posts to {@code calendars.events} with
 *       the current access token (auto-refreshing on 401).</li>
 * </ol>
 */
@Component
@ConditionalOnProperty(
        name = "edss.features.integrations.calendar.google-sync",
        havingValue = "true")
public class GoogleCalendarClient {

    private static final Logger log = LoggerFactory.getLogger(GoogleCalendarClient.class);
    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String EVENTS_URL =
            "https://www.googleapis.com/calendar/v3/calendars/primary/events";
    private static final String SCOPE = "https://www.googleapis.com/auth/calendar.events";

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper objectMapper;
    private final CalendarProperties.Google config;
    private final GoogleCalendarTokenRepository tokens;
    private final SecretCipher cipher;
    private final Clock clock;

    public GoogleCalendarClient(
            CalendarProperties properties,
            ObjectMapper objectMapper,
            GoogleCalendarTokenRepository tokens,
            SecretCipher cipher,
            Clock clock) {
        this.config = properties.google();
        this.objectMapper = objectMapper;
        this.tokens = tokens;
        this.cipher = cipher;
        this.clock = clock;
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
                + "&scope=" + urlEncode(SCOPE)
                + "&state=" + urlEncode(state);
    }

    @Transactional
    public void exchangeCode(UUID staffUserId, String code) {
        String form =
                "code=" + urlEncode(code)
                        + "&client_id=" + urlEncode(config.oauthClientId())
                        + "&client_secret=" + urlEncode(config.oauthClientSecret())
                        + "&redirect_uri=" + urlEncode(config.redirectUri())
                        + "&grant_type=authorization_code";
        JsonNode payload = post(TOKEN_URL, form, null);
        String accessToken = payload.path("access_token").asText();
        String refreshToken = payload.path("refresh_token").asText();
        long expiresIn = payload.path("expires_in").asLong(3600);
        Instant now = clock.instant();
        Instant expiresAt = now.plusSeconds(expiresIn);
        String scope = payload.path("scope").asText(SCOPE);

        tokens.save(
                new GoogleCalendarToken(
                        staffUserId,
                        cipher.encrypt(accessToken),
                        cipher.encrypt(refreshToken),
                        expiresAt,
                        scope,
                        now));
    }

    @Transactional
    public void insertOnboardingEvent(
            UUID staffUserId, String summary, Instant scheduledAt, String meetingUrl) {
        Optional<GoogleCalendarToken> maybeToken = tokens.findById(staffUserId);
        if (maybeToken.isEmpty()) {
            log.info("Skip Google Calendar insert — no token for staff {}", staffUserId);
            return;
        }
        GoogleCalendarToken token = maybeToken.get();
        String accessToken = ensureFreshAccessToken(token);

        try {
            String body =
                    objectMapper.writeValueAsString(
                            Map.of(
                                    "summary", summary,
                                    "description", meetingUrl == null ? "" : meetingUrl,
                                    "start", Map.of("dateTime", scheduledAt.toString()),
                                    "end",
                                            Map.of(
                                                    "dateTime",
                                                    scheduledAt.plusSeconds(3600).toString())));
            HttpRequest req =
                    HttpRequest.newBuilder(URI.create(EVENTS_URL))
                            .header("Authorization", "Bearer " + accessToken)
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                            .build();
            HttpResponse<String> res =
                    http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (res.statusCode() >= 300) {
                log.warn(
                        "Google Calendar insert failed for staff {} [{}]: {}",
                        staffUserId,
                        res.statusCode(),
                        res.body());
            }
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(ex);
        } catch (Exception ex) {
            log.warn("Google Calendar insert threw", ex);
        }
    }

    private String ensureFreshAccessToken(GoogleCalendarToken token) {
        if (token.getExpiresAt().isAfter(clock.instant().plus(Duration.ofMinutes(1)))) {
            return cipher.decrypt(token.getAccessTokenEnc());
        }
        String refresh = cipher.decrypt(token.getRefreshTokenEnc());
        String form =
                "refresh_token=" + urlEncode(refresh)
                        + "&client_id=" + urlEncode(config.oauthClientId())
                        + "&client_secret=" + urlEncode(config.oauthClientSecret())
                        + "&grant_type=refresh_token";
        JsonNode payload = post(TOKEN_URL, form, null);
        String accessToken = payload.path("access_token").asText();
        long expiresIn = payload.path("expires_in").asLong(3600);
        Instant now = clock.instant();
        token.rotate(cipher.encrypt(accessToken), now.plusSeconds(expiresIn), now);
        return accessToken;
    }

    private JsonNode post(String url, String form, String bearer) {
        HttpRequest.Builder builder =
                HttpRequest.newBuilder(URI.create(url))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(form, StandardCharsets.UTF_8));
        if (bearer != null) {
            builder.header("Authorization", "Bearer " + bearer);
        }
        HttpResponse<String> res;
        try {
            res = http.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            // Underlying I/O — log the class of failure without leaking the URL body.
            log.warn("Google OAuth call I/O failed", ex);
            throw new IllegalStateException("Google OAuth call failed (transport).");
        }
        if (res.statusCode() >= 300) {
            // Never surface the response body via the exception chain — Google
            // 4xx bodies can contain partial access / refresh tokens or the
            // caller's own email, all of which then land in Sentry.
            log.warn(
                    "Google OAuth call failed status={} body_length={}",
                    res.statusCode(),
                    res.body() == null ? 0 : res.body().length());
            throw new IllegalStateException(
                    "Google OAuth call failed [status=" + res.statusCode() + "].");
        }
        try {
            return objectMapper.readTree(res.body());
        } catch (Exception ex) {
            throw new IllegalStateException("Google OAuth response parse failed.");
        }
    }

    private static String urlEncode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
