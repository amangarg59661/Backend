package com.edss.shared.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP-over-WebSocket for in-app notification push. Client subscribes to
 * {@code /user/queue/notifications} after connecting to {@code /ws/v1}.
 * Only wired when the in-app channel is enabled.
 */
@Configuration
@EnableWebSocketMessageBroker
@ConditionalOnProperty(
        name = "edss.features.notifications.channels.in-app",
        havingValue = "true",
        matchIfMissing = true)
class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/queue", "/topic");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/v1").setAllowedOriginPatterns("*");
    }
}
