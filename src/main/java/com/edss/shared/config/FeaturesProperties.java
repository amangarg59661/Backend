package com.edss.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Single source of truth for every feature flag in the app.
 *
 * <p><strong>Convention for future flags:</strong> add a boolean or narrow
 * enum-like string to the appropriate nested record. Prefer boolean for
 * two-state, string for provider-like selectors. Never scatter
 * {@code @ConditionalOnProperty} across ad-hoc paths — group them here so the
 * flag surface is discoverable in one file and the yml.</p>
 *
 * <p>All flags read from {@code application.yml} under {@code edss.features.*}
 * and can be overridden via env vars (e.g. {@code EDSS_FEATURES_AUTH_TWO_FACTOR=false}).</p>
 */
@ConfigurationProperties(prefix = "edss.features")
public record FeaturesProperties(Storage storage, Auth auth, Integrations integrations) {

    public record Storage(
            /** Provider label. Drives connection defaults + docs only. */
            String dbProvider,
            /** Flip in-memory stores → Redis-backed. Enable when scaling out. */
            boolean redisEnabled,
            /** Turn the outbox relay on/off. Off = events never publish. */
            boolean outboxRelay) {}

    public record Auth(
            /** Prompt users with TOTP challenge after correct password. */
            boolean twoFactor,
            /** Enforce login rate limit per email + per IP. */
            boolean rateLimit,
            /** Forgot-password endpoint issues reset tokens + emits event. */
            boolean passwordReset,
            /** Rotate refresh tokens on every refresh call. */
            boolean sessionRotation) {}

    public record Integrations(Convex convex, Mail mail) {

        public record Convex(
                /** Convex.dev client active. NOT a primary data store — only for
                 * separate document projections; primary DB stays Postgres. */
                boolean enabled,
                String url,
                String deployKey) {}

        public record Mail(
                /** mailhog | smtp | ses | sendgrid. Only mailhog / smtp wired in v1. */
                String provider) {}
    }
}
