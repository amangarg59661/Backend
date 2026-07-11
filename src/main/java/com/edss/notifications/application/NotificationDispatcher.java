package com.edss.notifications.application;

import com.edss.shared.config.NotificationRoutingProperties;
import com.edss.shared.events.EventEnvelope;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Single consumer of every domain event. Looks up the channels the event
 * type routes to (from {@link NotificationRoutingProperties}) and asks each
 * enabled {@link NotificationChannel} to deliver. Channels that are disabled
 * via feature flag are simply absent from the injected list — no branching
 * needed here.
 */
@Component
public class NotificationDispatcher {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatcher.class);

    private final NotificationRoutingProperties routing;
    private final NotificationRecipientResolver recipientResolver;
    private final NotificationCopyResolver copyResolver;
    private final Map<String, NotificationChannel> channelsById;

    public NotificationDispatcher(
            NotificationRoutingProperties routing,
            NotificationRecipientResolver recipientResolver,
            NotificationCopyResolver copyResolver,
            List<NotificationChannel> channels) {
        this.routing = routing;
        this.recipientResolver = recipientResolver;
        this.copyResolver = copyResolver;
        this.channelsById =
                channels.stream()
                        .collect(
                                java.util.stream.Collectors.toUnmodifiableMap(
                                        NotificationChannel::channelId, c -> c));
    }

    @ApplicationModuleListener
    public void on(EventEnvelope envelope) {
        List<String> targetChannels = routing.channelsFor(envelope.eventType());
        if (targetChannels.isEmpty()) {
            return;
        }
        Optional<NotificationChannel.NotificationRecipient> recipient =
                recipientResolver.resolve(envelope);
        if (recipient.isEmpty()) {
            log.debug("No recipient resolved for {}", envelope.eventType());
            return;
        }
        NotificationChannel.NotificationCopy copy = copyResolver.resolve(envelope);
        for (String channelId : targetChannels) {
            NotificationChannel channel = channelsById.get(channelId);
            if (channel == null) {
                continue;
            }
            try {
                channel.deliver(recipient.get(), envelope, copy);
            } catch (RuntimeException ex) {
                log.warn(
                        "Channel {} failed for event {}",
                        channelId,
                        envelope.eventType(),
                        ex);
            }
        }
    }
}
