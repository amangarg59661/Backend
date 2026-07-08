package com.edss.testsupport;

import com.redis.testcontainers.RedisContainer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Test containers for integration tests. Postgres always starts; Redis starts
 * only when a test opts in via {@link #registerRedisProperties(DynamicPropertyRegistry)}.
 * Day-1 the app runs on in-memory stores so most tests do not need Redis.
 */
public final class PostgresRedisContainers {

    public static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:14-alpine"))
                    .withDatabaseName("edss")
                    .withUsername("edss")
                    .withPassword("edss");

    private static volatile RedisContainer redis;

    static {
        POSTGRES.start();
    }

    private PostgresRedisContainers() {}

    public static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    /**
     * Opts into Redis for tests that exercise the {@code edss.redis.enabled=true}
     * code path. Starts a single container lazily.
     */
    public static void registerRedisProperties(DynamicPropertyRegistry registry) {
        RedisContainer container = ensureRedis();
        registry.add("edss.redis.enabled", () -> "true");
        registry.add("spring.data.redis.host", container::getHost);
        registry.add("spring.data.redis.port", () -> container.getMappedPort(6379));
    }

    private static RedisContainer ensureRedis() {
        RedisContainer local = redis;
        if (local != null) {
            return local;
        }
        synchronized (PostgresRedisContainers.class) {
            if (redis == null) {
                RedisContainer c = new RedisContainer(DockerImageName.parse("redis:7-alpine"));
                c.start();
                redis = c;
            }
            return redis;
        }
    }
}
