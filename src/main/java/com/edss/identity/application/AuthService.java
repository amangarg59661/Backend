package com.edss.identity.application;

import com.edss.identity.api.dto.LoginResponse;
import com.edss.identity.api.dto.RefreshResponse;
import com.edss.identity.api.dto.UserDto;
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
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwt;
    private final RateLimiter rateLimiter;
    private final SecurityProperties securityProperties;
    private final OutboxWriter outbox;
    private final Clock clock;

    public AuthService(
            UserRepository users,
            PermissionRepository permissions,
            SessionRepository sessions,
            RefreshTokenStore refreshTokens,
            TwoFactorChallengeStore twoFactorChallenges,
            PasswordEncoder passwordEncoder,
            JwtService jwt,
            RateLimiter rateLimiter,
            SecurityProperties securityProperties,
            OutboxWriter outbox,
            Clock clock) {
        this.users = users;
        this.permissions = permissions;
        this.sessions = sessions;
        this.refreshTokens = refreshTokens;
        this.twoFactorChallenges = twoFactorChallenges;
        this.passwordEncoder = passwordEncoder;
        this.jwt = jwt;
        this.rateLimiter = rateLimiter;
        this.securityProperties = securityProperties;
        this.outbox = outbox;
        this.clock = clock;
    }

    public LoginResponse login(String email, String password, String ipAddress, String userAgent) {
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

        // 2FA gate — v1 does not persist enrollment yet, so this branch stays
        // dormant until the settings feature enables users. Wire is ready.
        if (isTwoFactorEnabled(user)) {
            String challengeId = twoFactorChallenges.issue(user.getId());
            return LoginResponse.challenge(challengeId);
        }

        return issueFullSession(user, ipAddress, userAgent);
    }

    public LoginResponse verifyTwoFactor(String challengeId, String code, String ipAddress, String userAgent) {
        Optional<UUID> userId = twoFactorChallenges.consume(challengeId);
        if (userId.isEmpty()) {
            throw new ApiException(ApiErrorCode.INVALID_TOTP, "Challenge expired.");
        }
        User user =
                users.findById(userId.get())
                        .orElseThrow(
                                () ->
                                        new ApiException(
                                                ApiErrorCode.INVALID_TOTP, "Challenge expired."));
        // v1: no TOTP secret persisted yet. Placeholder accepts the sentinel
        // "000000" like MSW mocks so the FE flow is exercisable end-to-end.
        if (!"000000".equals(code)) {
            throw new ApiException(ApiErrorCode.INVALID_TOTP, "Incorrect code.");
        }
        return issueFullSession(user, ipAddress, userAgent);
    }

    public RefreshResponse refresh(String refreshToken) {
        RefreshTokenStore.Stored stored =
                refreshTokens
                        .consume(refreshToken)
                        .orElseThrow(
                                () ->
                                        new ApiException(
                                                ApiErrorCode.SESSION_EXPIRED, "Session expired."));

        User user =
                users.findById(stored.userId())
                        .orElseThrow(
                                () ->
                                        new ApiException(
                                                ApiErrorCode.SESSION_EXPIRED, "Session expired."));

        JwtService.IssuedToken access =
                jwt.issueAccessToken(
                        user.getId(),
                        user.getEmail(),
                        user.getPrimaryRole(),
                        user.isHasBothRoles(),
                        stored.sessionId());
        RefreshTokenStore.IssuedRefresh next =
                refreshTokens.issue(
                        user.getId(), stored.sessionId(), securityProperties.jwt().refreshTtl());
        return new RefreshResponse(
                access.token(),
                access.expiresAt().getEpochSecond(),
                next.token(),
                permissions.findPermissionStringsByUserId(user.getId()),
                stored.sessionId().toString());
    }

    public void logout(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokens.revoke(refreshToken);
        }
    }

    private LoginResponse issueFullSession(User user, String ipAddress, String userAgent) {
        Instant now = clock.instant();
        UUID sessionId = UUID.randomUUID();
        sessions.save(new Session(sessionId, user.getId(), userAgent, ipAddress, now));

        JwtService.IssuedToken access =
                jwt.issueAccessToken(
                        user.getId(),
                        user.getEmail(),
                        user.getPrimaryRole(),
                        user.isHasBothRoles(),
                        sessionId);
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
        List<String> perms = permissions.findPermissionStringsByUserId(user.getId());

        return LoginResponse.full(
                access.token(),
                access.expiresAt().getEpochSecond(),
                refresh.token(),
                userDto,
                perms,
                sessionId.toString());
    }

    private boolean isTwoFactorEnabled(User user) {
        return false;
    }

    private void enforceLoginRateLimit(String email, String ipAddress) {
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
