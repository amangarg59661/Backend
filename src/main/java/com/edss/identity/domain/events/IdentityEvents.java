package com.edss.identity.domain.events;

import com.edss.shared.events.DomainEvent;
import java.time.Instant;
import java.util.UUID;

/**
 * Concrete identity events published via the outbox. Payloads are records so
 * they serialise cleanly to the envelope's {@code payload} field.
 */
public final class IdentityEvents {

    private IdentityEvents() {}

    public record UserRegistered(
            UUID eventId,
            Instant occurredAt,
            UUID userId,
            String email,
            String primaryRole)
            implements DomainEvent {
        @Override
        public String eventType() {
            return "identity.user_registered";
        }

        @Override
        public int eventVersion() {
            return 1;
        }

        @Override
        public String aggregateType() {
            return "user";
        }

        @Override
        public UUID aggregateId() {
            return userId;
        }
    }

    public record UserLoggedIn(
            UUID eventId, Instant occurredAt, UUID userId, UUID sessionId, String ipAddress)
            implements DomainEvent {
        @Override
        public String eventType() {
            return "identity.user_logged_in";
        }

        @Override
        public int eventVersion() {
            return 1;
        }

        @Override
        public String aggregateType() {
            return "user";
        }

        @Override
        public UUID aggregateId() {
            return userId;
        }
    }

    public record PasswordResetRequested(
            UUID eventId, Instant occurredAt, UUID userId, String email, String plaintextToken)
            implements DomainEvent {
        @Override
        public String eventType() {
            return "identity.password_reset_requested";
        }

        @Override
        public int eventVersion() {
            return 1;
        }

        @Override
        public String aggregateType() {
            return "user";
        }

        @Override
        public UUID aggregateId() {
            return userId;
        }
    }

    public record TwoFactorEnabled(UUID eventId, Instant occurredAt, UUID userId)
            implements DomainEvent {
        @Override
        public String eventType() {
            return "identity.two_factor_enabled";
        }

        @Override
        public int eventVersion() {
            return 1;
        }

        @Override
        public String aggregateType() {
            return "user";
        }

        @Override
        public UUID aggregateId() {
            return userId;
        }
    }
}
