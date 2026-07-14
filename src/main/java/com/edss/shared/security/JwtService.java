package com.edss.shared.security;

import com.edss.shared.config.SecurityProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    /**
     * S-18: fixed issuer + audience. Signing key rotation, if we ever ship
     * multiple, would key off a {@code kid} header; today there is one key
     * and one deployment, so a fixed pair is enough.
     */
    public static final String ISSUER = "edss-backend";
    public static final String AUDIENCE = "edss-api";

    private final SecretKey key;
    private final SecurityProperties properties;
    private final Clock clock;

    public JwtService(SecurityProperties properties, Clock clock) {
        String raw = properties.jwt().secret();
        byte[] secret = raw == null ? new byte[0] : raw.getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            throw new IllegalStateException(
                    "edss.security.jwt.secret must be at least 32 bytes (256 bits) — got "
                            + secret.length
                            + " byte(s). Set the JWT_SECRET env var to a 32+ character"
                            + " random string; e.g. `openssl rand -base64 48`.");
        }
        this.key = Keys.hmacShaKeyFor(secret);
        this.properties = properties;
        this.clock = clock;
    }

    public IssuedToken issueAccessToken(
            UUID userId,
            String email,
            String primaryRole,
            boolean hasBothRoles,
            UUID sessionId,
            java.util.List<String> permissions) {
        Instant now = clock.instant();
        Instant expiresAt = now.plus(properties.jwt().accessTtl());
        String token =
                Jwts.builder()
                        .subject(userId.toString())
                        .issuer(ISSUER)
                        .audience()
                        .add(AUDIENCE)
                        .and()
                        .claims(
                                Map.of(
                                        "email", email,
                                        "primary_role", primaryRole,
                                        "has_both_roles", hasBothRoles,
                                        "session_id", sessionId.toString(),
                                        "permissions", permissions))
                        .issuedAt(java.util.Date.from(now))
                        .expiration(java.util.Date.from(expiresAt))
                        .signWith(key)
                        .compact();
        return new IssuedToken(token, expiresAt);
    }

    @SuppressWarnings("unchecked")
    public ParsedToken parse(String token) {
        try {
            Claims claims =
                    Jwts.parser()
                            .verifyWith(key)
                            .requireIssuer(ISSUER)
                            .requireAudience(AUDIENCE)
                            .build()
                            .parseSignedClaims(token)
                            .getPayload();
            Object rawPerms = claims.get("permissions");
            java.util.List<String> perms =
                    rawPerms instanceof java.util.List<?> l
                            ? l.stream().map(Object::toString).toList()
                            : java.util.List.of();
            return new ParsedToken(
                    UUID.fromString(claims.getSubject()),
                    (String) claims.get("email"),
                    (String) claims.get("primary_role"),
                    Boolean.TRUE.equals(claims.get("has_both_roles")),
                    UUID.fromString((String) claims.get("session_id")),
                    perms,
                    claims.getExpiration().toInstant());
        } catch (JwtException | IllegalArgumentException ex) {
            throw new InvalidJwtException(ex.getMessage());
        }
    }

    public record IssuedToken(String token, Instant expiresAt) {}

    public record ParsedToken(
            UUID userId,
            String email,
            String primaryRole,
            boolean hasBothRoles,
            UUID sessionId,
            java.util.List<String> permissions,
            Instant expiresAt) {}

    public static class InvalidJwtException extends RuntimeException {
        public InvalidJwtException(String message) {
            super(message);
        }
    }
}
