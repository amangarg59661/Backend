package com.edss.notifications.application;

import com.edss.notifications.domain.Notification;
import com.edss.notifications.infrastructure.NotificationRepository;
import com.edss.shared.config.MailProperties;
import com.edss.shared.events.EventEnvelope;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Clock;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Consumes {@code identity.password_reset_requested} events from the outbox
 * relay. Sends a reset email and creates an in-app notification row. Runs
 * transactionally per Spring Modulith's async listener contract, with retry
 * via {@code event_publication_log}.
 */
@Component
public class PasswordResetNotifier {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetNotifier.class);

    private final NotificationRepository notifications;
    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;
    private final Clock clock;

    public PasswordResetNotifier(
            NotificationRepository notifications,
            JavaMailSender mailSender,
            MailProperties mailProperties,
            Clock clock) {
        this.notifications = notifications;
        this.mailSender = mailSender;
        this.mailProperties = mailProperties;
        this.clock = clock;
    }

    @ApplicationModuleListener
    public void on(EventEnvelope envelope) {
        if (!"identity.password_reset_requested".equals(envelope.eventType())) {
            return;
        }
        JsonNode payload = (JsonNode) envelope.payload();
        UUID userId = UUID.fromString(payload.get("user_id").asText());
        String email = payload.get("email").asText();
        String resetToken = payload.get("reset_token").asText();

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailProperties.from());
        message.setTo(email);
        message.setSubject("Reset your EDSS password");
        message.setText(
                "Use this token to reset your password: " + resetToken + "\n\nExpires in 30 minutes.");
        try {
            mailSender.send(message);
        } catch (RuntimeException ex) {
            log.warn("Failed to send password reset email to {}", email, ex);
        }

        notifications.save(
                new Notification(
                        UUID.randomUUID(),
                        userId,
                        "info",
                        "Password reset requested",
                        "Check your email for a reset link.",
                        null,
                        clock.instant()));
    }
}
