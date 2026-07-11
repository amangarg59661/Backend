package com.edss.identity.application;

import com.edss.identity.domain.TrustedDevice;
import com.edss.identity.infrastructure.TrustedDeviceRepository;
import com.edss.shared.config.FeaturesProperties;
import com.edss.shared.security.TokenHashing;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Issues signed "remember this device" tokens that let a browser skip 2FA
 * until the configured TTL expires. Only the SHA-256 hash of the token is
 * stored; the raw token lives in a browser cookie.
 */
@Service
@Transactional
public class TrustedDeviceService {

    private final TrustedDeviceRepository devices;
    private final FeaturesProperties features;
    private final Clock clock;

    public TrustedDeviceService(
            TrustedDeviceRepository devices, FeaturesProperties features, Clock clock) {
        this.devices = devices;
        this.features = features;
        this.clock = clock;
    }

    public IssuedDevice issue(UUID userId, String userAgent, String ipAddress) {
        String token = TokenHashing.randomUrlBase64(32);
        Duration ttl = Duration.ofDays(features.auth().rememberDeviceDays());
        Instant now = clock.instant();
        Instant expiresAt = now.plus(ttl);
        TrustedDevice device =
                new TrustedDevice(
                        UUID.randomUUID(),
                        userId,
                        TokenHashing.sha256UrlBase64(token),
                        userAgent,
                        ipAddress,
                        now,
                        expiresAt);
        devices.save(device);
        return new IssuedDevice(device.getId(), token, expiresAt);
    }

    public boolean isTrusted(UUID userId, String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        Optional<TrustedDevice> device =
                devices.findByDeviceTokenHash(TokenHashing.sha256UrlBase64(token));
        return device.filter(d -> d.getUserId().equals(userId))
                .filter(d -> d.isActive(clock.instant()))
                .isPresent();
    }

    @Transactional(readOnly = true)
    public List<TrustedDevice> list(UUID userId) {
        return devices.findByUserIdAndRevokedAtIsNull(userId);
    }

    public void revoke(UUID userId, UUID deviceId) {
        devices.findById(deviceId)
                .filter(d -> d.getUserId().equals(userId))
                .ifPresent(d -> d.revoke(clock.instant()));
    }

    public record IssuedDevice(UUID id, String token, Instant expiresAt) {}
}
