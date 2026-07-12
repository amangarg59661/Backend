package com.edss.shared.config;

import com.edss.shared.security.JwtService;
import java.security.Principal;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP-over-WebSocket for in-app notification push.
 *
 * <p>Auth: every CONNECT frame must carry {@code Authorization: Bearer &lt;jwt&gt;}
 * in the STOMP headers. The interceptor verifies the token via {@link JwtService}
 * and installs a {@link Principal} whose {@code getName()} returns the user id,
 * so {@code SimpMessagingTemplate.convertAndSendToUser(userId, "/queue/...")}
 * routes to the correct subscriber. Anonymous connects are rejected outright.
 *
 * <p>Origins: sourced from {@link SecurityProperties#cors()}. Wildcard origin
 * removed — an unauthenticated WS endpoint reachable from any origin is the
 * classic cross-site takeover shape.
 */
@Configuration
@EnableWebSocketMessageBroker
@ConditionalOnProperty(
        name = "edss.features.notifications.channels.in-app",
        havingValue = "true",
        matchIfMissing = true)
class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final Logger log = LoggerFactory.getLogger(WebSocketConfig.class);

    private final JwtService jwtService;
    private final SecurityProperties security;

    WebSocketConfig(JwtService jwtService, SecurityProperties security) {
        this.jwtService = jwtService;
        this.security = security;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/queue", "/topic");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        List<String> origins = security.cors().allowedOrigins();
        registry.addEndpoint("/ws/v1").setAllowedOrigins(origins.toArray(String[]::new));
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new StompAuthInterceptor());
    }

    private final class StompAuthInterceptor implements ChannelInterceptor {
        @Override
        public Message<?> preSend(Message<?> message, MessageChannel channel) {
            StompHeaderAccessor accessor =
                    MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
            if (accessor == null) {
                return message;
            }
            if (!StompCommand.CONNECT.equals(accessor.getCommand())) {
                return message;
            }
            String header = accessor.getFirstNativeHeader("Authorization");
            if (header == null || !header.startsWith("Bearer ")) {
                log.debug("Rejecting WS CONNECT with missing/invalid Authorization header");
                throw new SecurityException("Missing bearer token on WS CONNECT.");
            }
            String token = header.substring("Bearer ".length()).trim();
            JwtService.ParsedToken parsed;
            try {
                parsed = jwtService.parse(token);
            } catch (JwtService.InvalidJwtException ex) {
                log.debug("Rejecting WS CONNECT with invalid JWT: {}", ex.getMessage());
                throw new SecurityException("Invalid bearer token on WS CONNECT.");
            }
            String userId = parsed.userId().toString();
            accessor.setUser(new StompPrincipal(userId));
            return message;
        }
    }

    private record StompPrincipal(String name) implements Principal {
        @Override
        public String getName() {
            return name;
        }
    }
}
