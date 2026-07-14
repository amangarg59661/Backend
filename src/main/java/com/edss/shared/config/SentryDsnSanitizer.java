package com.edss.shared.config;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Boot-time guard against a placeholder Sentry DSN (e.g. the literal
 * {@code https://<key>@<org>.ingest.sentry.io/<project>} value that appears in
 * {@code .env.production.example}). If Sentry's Java client sees a DSN whose
 * authority contains reserved URI characters like {@code <} or {@code >}, it
 * throws {@link java.net.URISyntaxException} out of the {@code sentryHub} bean
 * factory and the whole Spring context fails to refresh.
 *
 * <p>This processor runs before Spring Boot's autoconfiguration reads the
 * environment. When the DSN looks like a placeholder, it force-disables Sentry
 * ({@code sentry.enabled=false}) and clears the DSN so the autoconfig sees no
 * value to parse. Boot proceeds without observability rather than crashing.
 *
 * <p>Registered via
 * {@code META-INF/spring/org.springframework.boot.env.EnvironmentPostProcessor.imports}.
 */
public class SentryDsnSanitizer implements EnvironmentPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(SentryDsnSanitizer.class);
    private static final String SOURCE_NAME = "sentryDsnSanitizerOverrides";

    @Override
    public void postProcessEnvironment(
            ConfigurableEnvironment environment, SpringApplication application) {
        String dsn = environment.getProperty("sentry.dsn");
        if (dsn == null) {
            return;
        }
        String trimmed = dsn.trim();
        if (trimmed.isEmpty() || looksLikePlaceholder(trimmed)) {
            Map<String, Object> overrides = new HashMap<>();
            overrides.put("sentry.enabled", "false");
            overrides.put("sentry.dsn", "");
            environment
                    .getPropertySources()
                    .addFirst(new MapPropertySource(SOURCE_NAME, overrides));
            log.warn(
                    "SENTRY_DSN looks like a placeholder or is blank (value hidden). "
                            + "Forcing sentry.enabled=false. Set a real DSN or leave SENTRY_ENABLED=false"
                            + " to silence this warning.");
        }
    }

    private static boolean looksLikePlaceholder(String dsn) {
        return dsn.indexOf('<') >= 0 || dsn.indexOf('>') >= 0;
    }
}
