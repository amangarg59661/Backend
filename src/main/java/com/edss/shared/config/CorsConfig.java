package com.edss.shared.config;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
class CorsConfig {

    private final SecurityProperties properties;

    CorsConfig(SecurityProperties properties) {
        this.properties = properties;
    }

    @Bean
    CorsFilter corsFilter() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(properties.cors().allowedOrigins());
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Request-Id", "Accept"));
        cfg.setExposedHeaders(List.of("X-Request-Id", "X-Api-Version", "Retry-After"));
        cfg.setAllowCredentials(false);
        cfg.setMaxAge(3600L);

        // S-24: scope CORS to /api/**. Webhooks are server-to-server (no
        // browser origin), actuator + swagger are internal. Explicitly
        // registering /api/** means those paths receive no Access-Control
        // headers — a stray fetch() from a malicious origin can't even
        // pre-flight, let alone hit them.
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cfg);
        return new CorsFilter(source);
    }
}
