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

    private final SecretKey key;
    private final SecurityProperties properties;
    private final Clock clock;

    public JwtService(SecurityProperties properties, Clock clock) {
        byte[] secret = properties.jwt().secret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            throw new IllegalStateException(
                    "edss.security.jwt.secret must be at least 32 bytes (256 bits).");
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
            UUID sessionId) {
        Instant now = clock.instant();
        Instant expiresAt = now.plus(properties.jwt().accessTtl());
        String token =
                Jwts.builder()
                        .subject(userId.toString())
                        .claims(
                                Map.of(
                                        "email", email,
                                        "primary_role", primaryRole,
                                        "has_both_roles", hasBothRoles,
                                        "session_id", sessionId.toString()))
                        .issuedAt(java.util.Date.from(now))
                        .expiration(java.util.Date.from(expiresAt))
                        .signWith(key)
                        .compact();
        return new IssuedToken(token, expiresAt);
    }

    public ParsedToken parse(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            return new ParsedToken(
                    UUID.fromString(claims.getSubject()),
                    (String) claims.get("email"),
                    (String) claims.get("primary_role"),
                    Boolean.TRUE.equals(claims.get("has_both_roles")),
                    UUID.fromString((String) claims.get("session_id")),
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
            Instant expiresAt) {}

    public static class InvalidJwtException extends RuntimeException {
        public InvalidJwtException(String message) {
            super(message);
        }
    }
}
