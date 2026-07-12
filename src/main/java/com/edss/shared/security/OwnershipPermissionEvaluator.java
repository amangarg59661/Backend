package com.edss.shared.security;

import java.io.Serializable;
import java.util.UUID;
import java.util.function.Predicate;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

/**
 * Custom Spring Security {@link PermissionEvaluator} used with {@code
 * @PreAuthorize("hasPermission(#resource, 'projects:project:read')")}. Handles
 * both plain permissions and {@code :own} suffixes.
 *
 * <p>Match rules:</p>
 * <ol>
 *   <li>Authority contains the exact permission → allow.</li>
 *   <li>Authority contains a wildcard the requested permission matches (e.g.
 *       {@code projects:*} vs {@code projects:project:read}) → allow.</li>
 *   <li>Requested permission ends in {@code :own} and the target has a
 *       {@code getOwnerUserId()} or {@code getClientUserId()} matching the
 *       principal → allow.</li>
 * </ol>
 */
@Component
public class OwnershipPermissionEvaluator implements PermissionEvaluator {

    private static final String OWN_SUFFIX = ":own";

    @Override
    public boolean hasPermission(Authentication auth, Object target, Object permission) {
        if (auth == null || !auth.isAuthenticated() || permission == null) {
            return false;
        }
        String required = permission.toString();
        String requiredBase = required.endsWith(OWN_SUFFIX)
                ? required.substring(0, required.length() - OWN_SUFFIX.length())
                : required;

        boolean anyGrant =
                auth.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .anyMatch(a -> matches(a, requiredBase) || matches(a, required));
        if (!anyGrant) {
            return false;
        }
        if (!required.endsWith(OWN_SUFFIX) || target == null) {
            return true;
        }
        Object principal = auth.getPrincipal();
        if (!(principal instanceof AuthenticatedUser user)) {
            return false;
        }
        return matchesOwnership(target, user.userId());
    }

    @Override
    public boolean hasPermission(
            Authentication auth, Serializable targetId, String targetType, Object permission) {
        return hasPermission(auth, targetId, permission);
    }

    /**
     * A single explicit grant that authorises any permission check. Reserved
     * for genuine break-glass access — do not seed it broadly, and audit every
     * hit that reaches it (log at WARN in the caller).
     */
    private static final String SUPER_OVERRIDE = "admin:override";

    private boolean matches(String granted, String required) {
        if (granted.equals(required)) {
            return true;
        }
        if (SUPER_OVERRIDE.equals(granted)) {
            return true;
        }
        if (!granted.endsWith(":*")) {
            return false;
        }
        String prefix = granted.substring(0, granted.length() - 1);
        return required.startsWith(prefix);
    }

    private boolean matchesOwnership(Object target, UUID userId) {
        return firstAvailableOwner(target).test(userId);
    }

    private Predicate<UUID> firstAvailableOwner(Object target) {
        UUID candidate = probe(target, "getOwnerUserId");
        if (candidate == null) {
            candidate = probe(target, "getClientUserId");
        }
        if (candidate == null) {
            candidate = probe(target, "getRaisedByUserId");
        }
        if (candidate == null) {
            candidate = probe(target, "getUserId");
        }
        UUID owner = candidate;
        return principalId -> owner != null && owner.equals(principalId);
    }

    private UUID probe(Object target, String method) {
        try {
            Object value = target.getClass().getMethod(method).invoke(target);
            return value instanceof UUID id ? id : null;
        } catch (ReflectiveOperationException ex) {
            return null;
        }
    }
}
