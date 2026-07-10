package com.edss.identity.application;

import com.edss.identity.domain.Permission;
import com.edss.identity.domain.Role;
import com.edss.identity.domain.User;
import com.edss.identity.infrastructure.PermissionRepository;
import com.edss.identity.infrastructure.UserRepository;
import com.edss.shared.config.AdminSeedProperties;
import java.time.Clock;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ensures the admin user exists after Flyway runs. Idempotent — safe to run
 * every boot. Fails fast if the seed password is not configured so a fresh
 * env never boots without credentials.
 */
@Component
public class AdminSeedRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeedRunner.class);

    private final UserRepository users;
    private final PermissionRepository permissions;
    private final PasswordEncoder passwordEncoder;
    private final AdminSeedProperties adminProperties;
    private final PermissionCatalog permissionCatalog;
    private final Clock clock;

    public AdminSeedRunner(
            UserRepository users,
            PermissionRepository permissions,
            PasswordEncoder passwordEncoder,
            AdminSeedProperties adminProperties,
            PermissionCatalog permissionCatalog,
            Clock clock) {
        this.users = users;
        this.permissions = permissions;
        this.passwordEncoder = passwordEncoder;
        this.adminProperties = adminProperties;
        this.permissionCatalog = permissionCatalog;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(String... args) {
        String email = adminProperties.email();
        String password = adminProperties.password();
        if (email == null || email.isBlank()) {
            log.warn("Admin email not configured; skipping seed.");
            return;
        }
        if (users.existsByEmailIgnoreCase(email)) {
            return;
        }
        if (password == null || password.isBlank()) {
            throw new IllegalStateException(
                    "edss.admin.password must be set to seed the initial admin.");
        }
        UUID id = UUID.randomUUID();
        User admin =
                new User(
                        id,
                        email,
                        "Administrator",
                        passwordEncoder.encode(password),
                        "staff",
                        true,
                        true,
                        clock.instant());
        users.save(admin);
        for (String perm : permissionCatalog.permissionsFor(Role.ADMIN)) {
            permissions.save(new Permission(id, perm));
        }
        log.info("Seeded admin user {} ({})", email, id);
    }
}
