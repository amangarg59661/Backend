package com.edss.identity.application;

import com.edss.identity.domain.Role;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Canonical permission grants per role. Consulted at user creation to seed
 * {@code identity.user_permissions}. Editing a role's permissions here
 * changes the grant for <em>new</em> users only — existing users keep the
 * permissions they were granted at creation. Runbook: promote a user by
 * inserting the additional permissions manually or via a promotion endpoint.
 *
 * <p>The {@code :own} suffix is honoured by {@link OwnershipPermissionEvaluator}
 * — a permission like {@code projects:project:read:own} lets the user read
 * only projects they own.</p>
 */
@Component
public class PermissionCatalog {

    private static final Map<Role, List<String>> GRANTS =
            Map.of(
                    Role.ADMIN,
                    List.of("admin:*"),
                    Role.STAFF,
                    List.of(
                            "projects:project:read",
                            "commitments:ticket:read",
                            "notifications:notification:*:own"),
                    Role.PROJECT_MANAGER,
                    List.of(
                            "projects:*",
                            "commitments:ticket:read",
                            "commitments:ticket:reply",
                            "knowledge:file:*",
                            "relationship:inquiry:*",
                            "notifications:*"),
                    Role.SUPPORT_AGENT,
                    List.of(
                            "commitments:ticket:*",
                            "knowledge:file:read",
                            "projects:project:read",
                            "notifications:*"),
                    Role.ACCOUNTANT,
                    List.of(
                            "finance:*",
                            "projects:project:read",
                            "notifications:*"),
                    Role.CLIENT,
                    List.of(
                            "projects:project:read:own",
                            "commitments:ticket:read:own",
                            "commitments:ticket:create:own",
                            "commitments:ticket:reply:own",
                            "knowledge:file:read:own",
                            "knowledge:file:upload:own",
                            "finance:invoice:read:own",
                            "finance:invoice:pay:own",
                            "notifications:notification:*:own"));

    public List<String> permissionsFor(Role role) {
        return GRANTS.getOrDefault(role, List.of());
    }

    public List<String> permissionsForWire(String wire) {
        return Role.ofWire(wire).map(this::permissionsFor).orElse(List.of());
    }
}
