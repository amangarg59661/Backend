package com.edss.integrations.convex;

import com.edss.shared.config.FeaturesProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Convex.dev client stub. Reserved integration point — not wired into any
 * domain module yet. Convex is a document DB with a JS-first SDK; the Java
 * side would call the HTTP action API. Enable via
 * {@code edss.features.integrations.convex.enabled=true} to instantiate.
 *
 * <p>Convex is <strong>not</strong> a primary data store. Postgres remains
 * authoritative. Use this client only for secondary projections (e.g. a fast
 * document view of a domain object) and always project via events, not
 * dual-write.</p>
 */
@Component
@ConditionalOnProperty(
        name = "edss.features.integrations.convex.enabled",
        havingValue = "true")
public class ConvexClient {

    private static final Logger log = LoggerFactory.getLogger(ConvexClient.class);

    private final FeaturesProperties.Integrations.Convex config;

    public ConvexClient(FeaturesProperties features) {
        this.config = features.integrations().convex();
        if (config.url() == null || config.url().isBlank()) {
            throw new IllegalStateException(
                    "edss.features.integrations.convex.url must be set when convex.enabled=true.");
        }
        log.info("Convex client enabled for deployment {}", config.url());
    }

    /** Placeholder — real impl posts to {@code {url}/api/action}. */
    public void runAction(String actionName, Object args) {
        throw new UnsupportedOperationException(
                "Convex integration not yet wired. Implement via HTTP action API.");
    }
}
