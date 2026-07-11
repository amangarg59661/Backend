package com.edss.notifications.application;

import com.edss.identity.spi.IdentityQuery;
import com.edss.notifications.application.NotificationChannel.NotificationRecipient;
import com.edss.shared.events.EventEnvelope;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Resolves the addressed user from an event envelope. Uses the first
 * available id field in the payload (checked in a fixed order:
 * {@code user_id}, {@code owner_user_id}, {@code client_user_id},
 * {@code raised_by_user_id}) and hydrates via {@link IdentityQuery}.
 */
@Component
public class NotificationRecipientResolver {

    private static final String[] USER_ID_KEYS = {
        "user_id", "owner_user_id", "client_user_id", "raised_by_user_id"
    };

    private final IdentityQuery identity;

    public NotificationRecipientResolver(IdentityQuery identity) {
        this.identity = identity;
    }

    public Optional<NotificationRecipient> resolve(EventEnvelope envelope) {
        JsonNode payload = (JsonNode) envelope.payload();
        UUID userId = null;
        for (String key : USER_ID_KEYS) {
            JsonNode value = payload.path(key);
            if (!value.isMissingNode() && !value.asText().isBlank()) {
                try {
                    userId = UUID.fromString(value.asText());
                    break;
                } catch (IllegalArgumentException ignore) {
                    // continue to next key
                }
            }
        }
        if (userId == null) {
            return Optional.empty();
        }
        return identity.findUser(userId)
                .map(u -> new NotificationRecipient(u.id(), u.email(), u.name(), null));
    }
}
