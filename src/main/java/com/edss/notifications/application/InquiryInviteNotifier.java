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
 * Sends the invite email when staff converts an inquiry to a client account.
 * Payload carries the plaintext invite token (single-use, 7-day TTL) that
 * the recipient uses on {@code POST /api/v1/auth/reset-password} to set
 * their password on first sign-in.
 */
@Component
public class InquiryInviteNotifier {

    private static final Logger log = LoggerFactory.getLogger(InquiryInviteNotifier.class);

    private final NotificationRepository notifications;
    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;
    private final Clock clock;

    public InquiryInviteNotifier(
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
        if (!"relationship.inquiry_converted".equals(envelope.eventType())) {
            return;
        }
        JsonNode payload = (JsonNode) envelope.payload();
        UUID userId = UUID.fromString(payload.get("user_id").asText());
        String email = payload.get("email").asText();
        String name = payload.get("name").asText();
        String inviteToken = payload.get("invite_token").asText();

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailProperties.from());
        message.setTo(email);
        message.setSubject("Welcome to EDSS — set your password");
        message.setText(
                "Hi "
                        + name
                        + ",\n\nYour account is ready. Set your password using this token:\n"
                        + inviteToken
                        + "\n\nThis invite expires in 7 days.");
        try {
            mailSender.send(message);
        } catch (RuntimeException ex) {
            log.warn("Failed to send invite email to {}", email, ex);
        }

        notifications.save(
                new Notification(
                        UUID.randomUUID(),
                        userId,
                        "info",
                        "Account created",
                        "Check your email for a set-password link.",
                        null,
                        clock.instant()));
    }
}
