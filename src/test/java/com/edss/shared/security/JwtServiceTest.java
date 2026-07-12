package com.edss.shared.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.edss.shared.config.SecurityProperties;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final SecurityProperties PROPS =
            new SecurityProperties(
                    new SecurityProperties.Jwt(
                            "test-secret-that-is-definitely-at-least-32-bytes!!",
                            Duration.ofMinutes(15),
                            Duration.ofDays(30)),
                    new SecurityProperties.Cors(List.of("http://localhost:3001")),
                    new SecurityProperties.RateLimit(5, 20, Duration.ofMinutes(15)),
                    new SecurityProperties.Secrets(
                            "YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXoxMjM0NTY=", null, null));

    private final JwtService jwt = new JwtService(PROPS, Clock.systemUTC());

    @Test
    void issuedTokenIsParseable() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        JwtService.IssuedToken issued =
                jwt.issueAccessToken(
                        userId,
                        "user@example.com",
                        "staff",
                        false,
                        sessionId,
                        List.of("projects:project:read", "admin:*"));
        JwtService.ParsedToken parsed = jwt.parse(issued.token());

        assertThat(parsed.userId()).isEqualTo(userId);
        assertThat(parsed.email()).isEqualTo("user@example.com");
        assertThat(parsed.primaryRole()).isEqualTo("staff");
        assertThat(parsed.hasBothRoles()).isFalse();
        assertThat(parsed.sessionId()).isEqualTo(sessionId);
        assertThat(parsed.permissions()).containsExactly("projects:project:read", "admin:*");
    }

    @Test
    void garbageTokenIsRejected() {
        assertThatThrownBy(() -> jwt.parse("not-a-jwt"))
                .isInstanceOf(JwtService.InvalidJwtException.class);
    }

    @Test
    void issuedTokenCarriesIssuerAndAudience() {
        JwtService.IssuedToken issued =
                jwt.issueAccessToken(
                        UUID.randomUUID(),
                        "u@example.com",
                        "staff",
                        false,
                        UUID.randomUUID(),
                        List.of());
        // Decode the JWT payload manually to avoid depending on parse() side effects.
        String[] parts = issued.token().split("\\.");
        String payloadJson =
                new String(
                        java.util.Base64.getUrlDecoder().decode(parts[1]),
                        java.nio.charset.StandardCharsets.UTF_8);
        assertThat(payloadJson).contains("\"iss\":\"" + JwtService.ISSUER + "\"");
        assertThat(payloadJson).contains("\"aud\":");
    }

    @Test
    void tokenWithWrongIssuerIsRejected() {
        // Forge a JWT signed with the same key but a different issuer using
        // JJWT directly. The parser must fail requireIssuer().
        javax.crypto.SecretKey key =
                io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                        PROPS.jwt()
                                .secret()
                                .getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String forged =
                io.jsonwebtoken.Jwts.builder()
                        .subject(UUID.randomUUID().toString())
                        .issuer("evil")
                        .audience()
                        .add(JwtService.AUDIENCE)
                        .and()
                        .expiration(new java.util.Date(System.currentTimeMillis() + 60_000))
                        .signWith(key)
                        .compact();
        assertThatThrownBy(() -> jwt.parse(forged))
                .isInstanceOf(JwtService.InvalidJwtException.class);
    }
}
