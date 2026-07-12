package com.edss.integrations.calendar.api;

import com.edss.integrations.calendar.GoogleCalendarClient;
import com.edss.shared.api.ApiErrorCode;
import com.edss.shared.api.ApiException;
import com.edss.shared.security.AuthenticatedUser;
import com.edss.shared.security.EphemeralSecrets;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Per-staff Google Calendar OAuth. {@code /authorize} returns the URL the
 * frontend redirects to; {@code /callback} completes the code exchange.
 * Only wired when {@code google-sync=true}.
 */
@RestController
@RequestMapping("/api/v1/integrations/google")
@ConditionalOnProperty(
        name = "edss.features.integrations.calendar.google-sync",
        havingValue = "true")
@Tag(name = "google-calendar", description = "Per-staff Google Calendar OAuth.")
public class GoogleCalendarController {

    private static final String STATE_PREFIX = "google_oauth_state:";
    private static final Duration STATE_TTL = Duration.ofMinutes(10);

    private final GoogleCalendarClient client;
    private final EphemeralSecrets secrets;

    public GoogleCalendarController(GoogleCalendarClient client, EphemeralSecrets secrets) {
        this.client = client;
        this.secrets = secrets;
    }

    @GetMapping("/authorize")
    @PreAuthorize("isAuthenticated()")
    public Map<String, String> authorize(@AuthenticationPrincipal AuthenticatedUser principal) {
        // S-19: state MUST be server-issued and one-time. Never accept a
        // client-supplied state — that lets an attacker forge callbacks.
        // Stash the principal's user id under the state so the callback proves
        // both that this OAuth dance is one we started and that it terminates
        // at the same authenticated user.
        String state = UUID.randomUUID().toString();
        secrets.stashUnder(STATE_PREFIX + state, principal.userId().toString(), STATE_TTL);
        return Map.of(
                "authorize_url", client.buildAuthorizeUrl(principal.userId(), state),
                "state", state);
    }

    @GetMapping("/callback")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> callback(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam String code,
            @RequestParam String state) {
        String stashedUserId =
                secrets.pop(STATE_PREFIX + state)
                        .orElseThrow(
                                () ->
                                        new ApiException(
                                                ApiErrorCode.FORBIDDEN,
                                                "Invalid or expired OAuth state."));
        if (!stashedUserId.equals(principal.userId().toString())) {
            throw new ApiException(ApiErrorCode.FORBIDDEN, "OAuth state / user mismatch.");
        }
        client.exchangeCode(principal.userId(), code);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
