package com.edss.notifications.application;

import com.edss.shared.config.MailProperties;
import com.edss.shared.events.EventEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "edss.features.notifications.channels.email",
        havingValue = "true",
        matchIfMissing = true)
public class EmailChannel implements NotificationChannel {

    public static final int BIT = 1;
    private static final Logger log = LoggerFactory.getLogger(EmailChannel.class);

    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;

    public EmailChannel(JavaMailSender mailSender, MailProperties mailProperties) {
        this.mailSender = mailSender;
        this.mailProperties = mailProperties;
    }

    @Override
    public String channelId() {
        return "email";
    }

    @Override
    public int bitMask() {
        return BIT;
    }

    @Override
    public void deliver(NotificationRecipient recipient, EventEnvelope envelope, NotificationCopy copy) {
        if (recipient.email() == null || recipient.email().isBlank()) {
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailProperties.from());
        message.setTo(recipient.email());
        message.setSubject(copy.title());
        message.setText(copy.body());
        try {
            mailSender.send(message);
        } catch (RuntimeException ex) {
            log.warn("Email delivery failed for {}: {}", recipient.email(), ex.getMessage());
        }
    }
}
