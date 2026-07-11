package com.edss.identity.application;

import com.edss.identity.domain.PasswordResetToken;
import com.edss.identity.domain.Permission;
import com.edss.identity.domain.Role;
import com.edss.identity.domain.User;
import com.edss.identity.domain.events.IdentityEvents;
import com.edss.identity.infrastructure.PasswordResetTokenRepository;
import com.edss.identity.infrastructure.PermissionRepository;
import com.edss.identity.infrastructure.UserRepository;
import com.edss.identity.spi.IdentityUserProvisioning;
import com.edss.shared.events.OutboxWriter;
import com.edss.shared.security.TokenHashing;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Concrete impl of {@link IdentityUserProvisioning}. Creates the user +
 * permission grants + an invite token (identical to a password-reset token,
 * TTL 7 days). Emits {@code identity.user_registered} on the identity outbox
 * so downstream modules can react.
 */
@Service
@Transactional
class IdentityUserProvisioningService implements IdentityUserProvisioning {

    private static final Duration INVITE_TTL = Duration.ofDays(7);

    private final UserRepository users;
    private final PermissionRepository permissions;
    private final PasswordResetTokenRepository tokens;
    private final PermissionCatalog catalog;
    private final PasswordEncoder passwordEncoder;
    private final OutboxWriter outbox;
    private final Clock clock;

    IdentityUserProvisioningService(
            UserRepository users,
            PermissionRepository permissions,
            PasswordResetTokenRepository tokens,
            PermissionCatalog catalog,
            PasswordEncoder passwordEncoder,
            OutboxWriter outbox,
            Clock clock) {
        this.users = users;
        this.permissions = permissions;
        this.tokens = tokens;
        this.catalog = catalog;
        this.passwordEncoder = passwordEncoder;
        this.outbox = outbox;
        this.clock = clock;
    }

    @Override
    public InviteResult createInvited(String email, String name, String primaryRole) {
        if (users.existsByEmailIgnoreCase(email)) {
            throw new EmailAlreadyExistsException(email);
        }
        Role role = Role.ofWire(primaryRole).orElse(Role.CLIENT);
        Instant now = clock.instant();
        UUID userId = UUID.randomUUID();
        // Random placeholder hash — never usable because BCrypt never matches
        // the empty string, and the invite flow forces a reset before login.
        String placeholderHash = passwordEncoder.encode(TokenHashing.randomUrlBase64(32));
        User user =
                new User(userId, email, name, placeholderHash, role.wire(), false, true, now);
        users.save(user);
        for (String perm : catalog.permissionsFor(role)) {
            permissions.save(new Permission(userId, perm));
        }

        String plaintextToken = TokenHashing.randomUrlBase64(32);
        String tokenHash = TokenHashing.sha256UrlBase64(plaintextToken);
        Instant expiresAt = now.plus(INVITE_TTL);
        tokens.save(new PasswordResetToken(tokenHash, userId, expiresAt));

        outbox.append(
                "identity",
                new IdentityEvents.UserRegistered(
                        UUID.randomUUID(), now, userId, email, role.wire()),
                Map.of("user_id", userId, "email", email, "primary_role", role.wire()));

        return new InviteResult(userId, plaintextToken, expiresAt);
    }

}
