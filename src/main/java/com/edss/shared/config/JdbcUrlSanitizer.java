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
 * Boot-time guard against a DB_URL that omits the mandatory {@code jdbc:} prefix.
 * Supabase (and every other managed Postgres) surfaces two connection strings
 * in its dashboard: a libpq-style {@code postgresql://…} (or {@code postgres://…})
 * URL for psql/pg clients, and a JDBC-form {@code jdbc:postgresql://…} URL for
 * Java clients. Operators paste the first one about half the time, then boot
 * dies with {@code java.lang.IllegalArgumentException: URL must start with 'jdbc'}
 * out of {@code DatabaseDriver.fromJdbcUrl} before Hikari even starts.
 *
 * <p>This processor runs before autoconfiguration reads the datasource
 * properties. When {@code spring.datasource.url} starts with {@code postgres://}
 * or {@code postgresql://}, it prepends {@code jdbc:} and re-publishes the
 * value at highest precedence. The Postgres JDBC driver accepts embedded
 * credentials in the URL, so a URL of the form
 * {@code postgres://user:pass@host:5432/db} works after prepending too.
 *
 * <p>Registered via
 * {@code META-INF/spring/org.springframework.boot.env.EnvironmentPostProcessor.imports}.
 */
public class JdbcUrlSanitizer implements EnvironmentPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(JdbcUrlSanitizer.class);
    private static final String SOURCE_NAME = "jdbcUrlSanitizerOverrides";

    @Override
    public void postProcessEnvironment(
            ConfigurableEnvironment environment, SpringApplication application) {
        String url = environment.getProperty("spring.datasource.url");
        if (url == null) {
            return;
        }
        String trimmed = url.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        String fixed;
        if (trimmed.startsWith("postgresql://")) {
            fixed = "jdbc:" + trimmed;
        } else if (trimmed.startsWith("postgres://")) {
            fixed = "jdbc:postgresql://" + trimmed.substring("postgres://".length());
        } else {
            return;
        }
        Map<String, Object> overrides = new HashMap<>();
        overrides.put("spring.datasource.url", fixed);
        environment
                .getPropertySources()
                .addFirst(new MapPropertySource(SOURCE_NAME, overrides));
        log.warn(
                "DB_URL was missing the mandatory 'jdbc:' prefix — normalized in place."
                        + " Update the env var to start with 'jdbc:postgresql://' to silence this"
                        + " warning. Any embedded username/password are still honoured by the driver.");
    }
}
