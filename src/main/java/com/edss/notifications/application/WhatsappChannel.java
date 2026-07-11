package com.edss.notifications.application;

import com.edss.integrations.messaging.TwilioWhatsappClient;
import com.edss.notifications.domain.WhatsappDelivery;
import com.edss.notifications.infrastructure.WhatsappDeliveryRepository;
import com.edss.shared.events.EventEnvelope;
import java.time.Clock;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "edss.features.notifications.channels.whatsapp",
        havingValue = "true")
public class WhatsappChannel implements NotificationChannel {

    public static final int BIT = 4;
    private static final Logger log = LoggerFactory.getLogger(WhatsappChannel.class);

    private final TwilioWhatsappClient twilio;
    private final WhatsappDeliveryRepository deliveries;
    private final Clock clock;

    public WhatsappChannel(
            TwilioWhatsappClient twilio,
            WhatsappDeliveryRepository deliveries,
            Clock clock) {
        this.twilio = twilio;
        this.deliveries = deliveries;
        this.clock = clock;
    }

    @Override
    public String channelId() {
        return "whatsapp";
    }

    @Override
    public int bitMask() {
        return BIT;
    }

    @Override
    public void deliver(NotificationRecipient recipient, EventEnvelope envelope, NotificationCopy copy) {
        if (recipient.phoneE164() == null || recipient.phoneE164().isBlank()) {
            log.debug("Skip WhatsApp — no phone for user {}", recipient.userId());
            return;
        }
        WhatsappDelivery row =
                new WhatsappDelivery(
                        UUID.randomUUID(), null, recipient.phoneE164(), clock.instant());
        deliveries.save(row);
        try {
            String body = copy.title() + " — " + copy.body();
            String sid = twilio.send(recipient.phoneE164(), body);
            row.markSent(sid);
        } catch (RuntimeException ex) {
            row.markFailed(ex.getMessage());
            log.warn("WhatsApp send failed for {}: {}", recipient.phoneE164(), ex.getMessage());
        }
    }
}
