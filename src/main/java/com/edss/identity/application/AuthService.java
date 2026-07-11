package com.edss.identity.application;

import com.edss.identity.api.dto.LoginResponse;
import com.edss.identity.api.dto.RefreshResponse;
import com.edss.identity.api.dto.UserDto;
import com.edss.identity.domain.MfaMethod;
import com.edss.identity.domain.MfaMethodType;
import com.edss.identity.domain.Session;
import com.edss.identity.domain.User;
import com.edss.identity.domain.events.IdentityEvents;
import com.edss.identity.infrastructure.PermissionRepository;
import com.edss.identity.infrastructure.RefreshTokenStore;
import com.edss.identity.infrastructure.SessionRepository;
import com.edss.identity.infrastructure.TwoFactorChallengeStore;
import com.edss.identity.infrastructure.UserRepository;
import com.edss.shared.api.ApiErrorCode;
import com.edss.shared.api.ApiException;
import com.edss.shared.config.FeaturesProperties;
import com.edss.shared.config.SecurityProperties;
import com.edss.shared.events.OutboxWriter;
import com.edss.shared.ratelimit.RateLimitDecision;
import com.edss.shared.ratelimit.RateLimiter;
import com.edss.shared.security.JwtService;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository users;
    private final PermissionRepository permissions;
    private final SessionRepository sessions;
    private final RefreshTokenStore refreshTokens;
    private final TwoFactorChallengeStore twoFactorChallenges;
    private final MfaMethodsService mfa;
    private final TrustedDeviceService trustedDevices;
    private final SessionService sessionService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwt;
    private final RateLimiter rateLimiter;
    private final SecurityProperties securityProperties;
    private final FeaturesProperties features;
    private final OutboxWriter outbox;
    private final Clock clock;

    public AuthService(
            UserRepository users,
            PermissionRepository permissions,
            SessionRepository sessions,
            RefreshTokenStore refreshTokens,
            TwoFactorChallengeStore twoFactorChallenges,
            MfaMethodsService mfa,
            TrustedDeviceService trustedDevices,
            SessionService sessionService,
            PasswordEncoder passwordEncoder,
            JwtService jwt,
            RateLimiter rateLimiter,
            SecurityProperties securityProperties,
            FeaturesProperties features,
            OutboxWriter outbox,
            Clock clock) {
        this.users = users;
        this.permissions = permissions;
        this.sessions = sessions;
        this.refreshTokens = refreshTokens;
        this.twoFactorChallenges = twoFactorChallenges;
        this.mfa = mfa;
        this.trustedDevices = trustedDevices;
        this.sessionService = sessionService;
        this.passwordEncoder = passwordEncoder;
        this.jwt = jwt;
        this.rateLimiter = rateLimiter;
        this.securityProperties = securityProperties;
        this.features = features;
        this.outbox = outbox;
        this.clock = clock;
    }

    public LoginResponse login(
            String email,
            String password,
            String trustedDeviceToken,
            String ipAddress,
            String userAgent) {
        enforceLoginRateLimit(email, ipAddress);

        Optional<User> maybeUser = users.findByEmailIgnoreCase(email);
        if (maybeUser.isEmpty()
                || !passwordEncoder.matches(password, maybeUser.get().getPasswordHash())) {
            throw new ApiException(ApiErrorCode.INVALID_CREDENTIALS, "Email or password incorrect.");
        }
        User user = maybeUser.get();
        if (!user.isActive()) {
            throw new ApiException(ApiErrorCode.FORBIDDEN, "Account is not active.");
        }

        List<MfaMethod> enabled = mfa.listEnabled(user.getId());
        boolean needsTwoFa =
                (features.auth().twoFactor() || !enabled.isEmpty())
                        && !trustedDevices.isTrusted(user.getId(), trustedDeviceToken);
        if (needsTwoFa && enabled.isEmpty()) {
            // Global flag forces 2FA but user never enrolled — fail loudly so
            // they hit the settings screen instead of a silent lockout.
            throw new ApiException(
                    ApiErrorCode.FORBIDDEN,
                    "Two-factor is required. Enroll a method in your account settings.");
        }
        if (needsTwoFa) {
            String challengeId = twoFactorChallenges.issue(user.getId());
            List<String> availableMethods =
                    enabled.stream().map(m -> m.getMethodType().wire()).toList();
            return LoginResponse.challenge(challengeId, availableMethods);
        }

        return issueFullSession(user, ipAddress, userAgent, false);
    }

    /**
     * Verify a chosen 2FA method's code + optionally issue a trusted-device
     * token. WhatsApp OTP calls this AFTER the client has already fetched the
     * OTP via {@link #requestWhatsappOtp(String)}.
     */
    public LoginResponse verifyTwoFactor(
            String challengeId,
            String methodWire,
            String code,
            boolean rememberDevice,
            String ipAddress,
            String userAgent) {
        MfaMethodType requestedType;
        try {
            requestedType = MfaMethodType.ofWire(methodWire);
        } catch (IllegalArgumentException ex) {
            throw new ApiException(ApiErrorCode.VALIDATION_FAILED, "Unknown MFA method.");
        }
        UUID userId =
                twoFactorChallenges
                        .consume(challengeId)
                        .orElseThrow(
                                () ->
                                        new ApiException(
                                                ApiErrorCode.INVALID_TOTP, "Challenge expired."));
        User user =
                users.findById(userId)
                        .orElseThrow(
                                () ->
                                        new ApiException(
                                                ApiErrorCode.INVALID_TOTP, "Challenge expired."));

        boolean ok =
                switch (requestedType) {
                    case TOTP -> mfa.findEnabled(user.getId(), MfaMethodType.TOTP)
                            .map(m -> mfa.verifyTotpForLogin(m, code))
                            .orElse(false);
                    case WHATSAPP_OTP -> mfa.findEnabled(user.getId(), MfaMethodType.WHATSAPP_OTP)
                            .map(m -> mfa.verifyWhatsappForLogin(m, code))
                            .orElse(false);
                    case BACKUP_CODE -> mfa.findEnabled(user.getId(), MfaMethodType.BACKUP_CODE)
                            .map(m -> mfa.verifyBackupCodeForLogin(user.getId(), code))
                            .orElse(false);
                };
        if (!ok) {
            throw new ApiException(ApiErrorCode.INVALID_TOTP, "Incorrect code.");
        }
        return issueFullSession(user, ipAddress, userAgent, rememberDevice);
    }

    /**
     * Client requests a WhatsApp OTP be sent as part of a live login challenge.
     * The challenge remains valid; verifyTwoFactor consumes it later.
     */
    public void requestWhatsappOtp(String challengeId) {
        UUID userId =
                twoFactorChallenges
                        .peek(challengeId)
                        .orElseThrow(
                                () ->
                                        new ApiException(
                                                ApiErrorCode.INVALID_TOTP, "Challenge expired."));
        MfaMethod method =
                mfa.findEnabled(userId, MfaMethodType.WHATSAPP_OTP)
                        .orElseThrow(
                                () ->
                                        new ApiException(
                                                ApiErrorCode.NOT_FOUND,
                                                "WhatsApp OTP not enrolled."));
        mfa.issueWhatsappOtpForLogin(method);
    }

    public RefreshResponse refresh(String refreshToken) {
        RefreshTokenStore.Stored stored =
                refreshTokens
                        .consume(refreshToken)
                        .orElseThrow(
                                () ->
                                        new ApiException(
                                                ApiErrorCode.SESSION_EXPIRED, "Session expired."));

        if (!sessionService.isActive(stored.sessionId())) {
            throw new ApiException(ApiErrorCode.SESSION_REVOKED, "Session revoked.");
        }
        User user =
                users.findById(stored.userId())
                        .orElseThrow(
                                () ->
                                        new ApiException(
                                                ApiErrorCode.SESSION_EXPIRED, "Session expired."));

        List<String> perms = permissions.findPermissionStringsByUserId(user.getId());
        JwtService.IssuedToken access =
                jwt.issueAccessToken(
                        user.getId(),
                        user.getEmail(),
                        user.getPrimaryRole(),
                        user.isHasBothRoles(),
                        stored.sessionId(),
                        perms);
        RefreshTokenStore.IssuedRefresh next =
                refreshTokens.issue(
                        user.getId(), stored.sessionId(), securityProperties.jwt().refreshTtl());
        return new RefreshResponse(
                access.token(),
                access.expiresAt().getEpochSecond(),
                next.token(),
                perms,
                stored.sessionId().toString());
    }

    public void logout(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokens.revoke(refreshToken);
        }
    }

    private LoginResponse issueFullSession(
            User user, String ipAddress, String userAgent, boolean rememberDevice) {
        Instant now = clock.instant();
        UUID sessionId = UUID.randomUUID();
        sessions.save(new Session(sessionId, user.getId(), userAgent, ipAddress, now));

        List<String> perms = permissions.findPermissionStringsByUserId(user.getId());
        JwtService.IssuedToken access =
                jwt.issueAccessToken(
                        user.getId(),
                        user.getEmail(),
                        user.getPrimaryRole(),
                        user.isHasBothRoles(),
                        sessionId,
                        perms);
        RefreshTokenStore.IssuedRefresh refresh =
                refreshTokens.issue(user.getId(), sessionId, securityProperties.jwt().refreshTtl());

        IdentityEvents.UserLoggedIn event =
                new IdentityEvents.UserLoggedIn(
                        UUID.randomUUID(), now, user.getId(), sessionId, ipAddress);
        outbox.append(
                "identity",
                event,
                Map.of(
                        "user_id",
                        user.getId(),
                        "session_id",
                        sessionId,
                        "ip_address",
                        ipAddress == null ? "" : ipAddress));

        UserDto userDto =
                new UserDto(
                        user.getId(),
                        user.getEmail(),
                        user.getName(),
                        user.getAvatarUrl(),
                        user.getPrimaryRole(),
                        user.isHasBothRoles(),
                        user.getCreatedAt());

        String trustedDeviceToken = null;
        if (rememberDevice) {
            TrustedDeviceService.IssuedDevice device =
                    trustedDevices.issue(user.getId(), userAgent, ipAddress);
            trustedDeviceToken = device.token();
        }

        return LoginResponse.full(
                access.token(),
                access.expiresAt().getEpochSecond(),
                refresh.token(),
                userDto,
                perms,
                sessionId.toString(),
                trustedDeviceToken);
    }

    private void enforceLoginRateLimit(String email, String ipAddress) {
        if (!features.auth().rateLimit()) {
            return;
        }
        int emailLimit = securityProperties.rateLimit().loginPerEmailPerWindow();
        int ipLimit = securityProperties.rateLimit().loginPerIpPerWindow();
        var window = securityProperties.rateLimit().window();

        RateLimitDecision byEmail = rateLimiter.hit("login:email:" + email.toLowerCase(), emailLimit, window);
        if (!byEmail.allowed()) {
            log.info("Login rate limit hit for email={}", email);
            throw new ApiException(
                    ApiErrorCode.RATE_LIMITED,
                    "Too many login attempts. Try again later.",
                    Map.of("retry_after", byEmail.retryAfter().getSeconds()));
        }
        if (ipAddress != null && !ipAddress.isBlank()) {
            RateLimitDecision byIp = rateLimiter.hit("login:ip:" + ipAddress, ipLimit, window);
            if (!byIp.allowed()) {
                log.info("Login rate limit hit for ip={}", ipAddress);
                throw new ApiException(
                        ApiErrorCode.RATE_LIMITED,
                        "Too many login attempts. Try again later.",
                        Map.of("retry_after", byIp.retryAfter().getSeconds()));
            }
        }
    }
}
