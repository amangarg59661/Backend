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
public record FeaturesProperties(
        Storage storage,
        Auth auth,
        Observability observability,
        Payments payments,
        Notifications notifications,
        Integrations integrations) {

    public record Storage(
            /** {@code supabase} (current) or {@code self-hosted} (future). Driver is
             * always Postgres; the label flows into logs + metrics. Real switching
             * happens by pointing {@code DB_URL} at the correct host. */
            String dbProvider,
            /** Flip in-memory stores → Redis-backed. Enable when scaling out. */
            boolean redisEnabled,
            /** Turn the outbox relay on/off. Off = events never publish. */
            boolean outboxRelay,
            /** File-storage backend: {@code supabase} or {@code s3}. */
            String fileBackend) {}

    public record Auth(
            /** Prompt users with TOTP challenge after correct password. */
            boolean twoFactor,
            /** Expose the 2FA enrollment endpoints. Distinct from twoFactor so
             * enrollment can stay open while login-side 2FA is being rolled out. */
            boolean twoFactorEnrollment,
            /** Enforce login rate limit per email + per IP. */
            boolean rateLimit,
            /** Forgot-password endpoint issues reset tokens + emits event. */
            boolean passwordReset,
            /** Rotate refresh tokens on every refresh call. */
            boolean sessionRotation,
            /** Days a "remember this device" token is valid. */
            int rememberDeviceDays) {}

    public record Observability(
            /** Ship uncaught exceptions to Sentry. Requires SENTRY_DSN when true. */
            boolean sentryEnabled) {}

    public record Payments(
            /** Stripe gateway available for invoice creation. */
            boolean stripeEnabled,
            /** Razorpay gateway available for invoice creation. */
            boolean razorpayEnabled,
            /** Manual gateway available (bank transfer / offline mark-paid). */
            boolean manualEnabled) {}

    public record Notifications(Channels channels) {

        public record Channels(
                /** Deliver notifications via SMTP. */
                boolean email,
                /** Deliver notifications into the in-app inbox + WebSocket push. */
                boolean inApp,
                /** Deliver notifications via Twilio WhatsApp Business. */
                boolean whatsapp,
                /** Deliver notifications via SMS. Stub until wired. */
                boolean sms) {}
    }

    public record Integrations(Mail mail, Calendar calendar, Messaging messaging) {

        public record Mail(
                /** mailhog | resend | smtp. Docs tag; SMTP host/port drive delivery. */
                String provider) {}

        public record Calendar(
                /** calcom | calendly | manual. Provider that mints booking links + emits webhooks. */
                String provider,
                /** Optionally sync onboarding calls into staff Google Calendar via OAuth. */
                boolean googleSync) {}

        public record Messaging(
                /** Twilio WhatsApp Business API on/off. */
                boolean whatsappEnabled) {}
    }
}
