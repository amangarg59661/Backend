package com.edss.integrations.calendar.api;

import com.edss.integrations.calendar.GoogleCalendarClient;
import com.edss.shared.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.tags.Tag;
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

    private final GoogleCalendarClient client;

    public GoogleCalendarController(GoogleCalendarClient client) {
        this.client = client;
    }

    @GetMapping("/authorize")
    @PreAuthorize("isAuthenticated()")
    public Map<String, String> authorize(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(required = false) String state) {
        String opaqueState = state == null ? UUID.randomUUID().toString() : state;
        return Map.of(
                "authorize_url", client.buildAuthorizeUrl(principal.userId(), opaqueState),
                "state", opaqueState);
    }

    @GetMapping("/callback")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> callback(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam String code,
            @RequestParam(required = false) String state) {
        client.exchangeCode(principal.userId(), code);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
