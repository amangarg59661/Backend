package com.edss.shared.config;

import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Event-to-channels routing. Populated from
 * {@code edss.notifications.routing.<event_type>: [email, in_app, whatsapp]}
 * in YAML so ops can retune without a redeploy of the code base.
 * Unknown events default to email + in_app.
 */
@ConfigurationProperties(prefix = "edss.notifications")
public record NotificationRoutingProperties(Map<String, List<String>> routing) {

    public List<String> channelsFor(String eventType) {
        if (routing == null) {
            return List.of("email", "in_app");
        }
        List<String> match = routing.get(eventType);
        return match == null ? List.of("email", "in_app") : match;
    }
}
