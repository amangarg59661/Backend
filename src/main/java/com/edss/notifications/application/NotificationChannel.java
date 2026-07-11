package com.edss.notifications.application;

import com.edss.shared.events.EventEnvelope;

/**
 * Delivery channel. Each impl is {@code @ConditionalOnProperty} on its
 * {@code edss.features.notifications.channels.<name>} flag so ops can
 * enable/disable per environment without a redeploy. Channels are called by
 * the {@link NotificationDispatcher} for every event in the routing map.
 */
public interface NotificationChannel {

    /** Channel identifier used as YAML value and DTO tag. */
    String channelId();

    /** Bit set on {@code notifications.channel_mask} when this channel fired. */
    int bitMask();

    void deliver(NotificationRecipient recipient, EventEnvelope envelope, NotificationCopy copy);

    record NotificationRecipient(
            java.util.UUID userId, String email, String name, String phoneE164) {}

    record NotificationCopy(String severity, String title, String body, String href) {}
}
