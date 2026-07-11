package com.edss.shared.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class OwnershipPermissionEvaluatorTest {

    private final OwnershipPermissionEvaluator evaluator = new OwnershipPermissionEvaluator();

    @Test
    void exactAuthorityGrants() {
        assertThat(
                        evaluator.hasPermission(
                                auth("projects:project:read"), null, "projects:project:read"))
                .isTrue();
    }

    @Test
    void wildcardAuthorityGrants() {
        assertThat(
                        evaluator.hasPermission(
                                auth("projects:*"), null, "projects:project:read"))
                .isTrue();
    }

    @Test
    void adminWildcardGrantsEverything() {
        assertThat(
                        evaluator.hasPermission(
                                auth("admin:*"), null, "finance:invoice:create"))
                .isTrue();
    }

    @Test
    void unrelatedAuthorityDenies() {
        assertThat(
                        evaluator.hasPermission(
                                auth("finance:*"), null, "projects:project:read"))
                .isFalse();
    }

    @Test
    void ownScopedPermissionRequiresOwnerMatch() {
        UUID userId = UUID.randomUUID();
        Authentication a = authWithPrincipal(userId, "projects:project:read:own");
        assertThat(
                        evaluator.hasPermission(
                                a, new HasOwner(userId), "projects:project:read:own"))
                .isTrue();
    }

    @Test
    void ownScopedPermissionDeniesWhenOwnerMismatch() {
        Authentication a = authWithPrincipal(UUID.randomUUID(), "projects:project:read:own");
        assertThat(
                        evaluator.hasPermission(
                                a,
                                new HasOwner(UUID.randomUUID()),
                                "projects:project:read:own"))
                .isFalse();
    }

    private static Authentication auth(String... authorities) {
        return new UsernamePasswordAuthenticationToken("p", "c", grants(authorities));
    }

    private static Authentication authWithPrincipal(UUID userId, String... authorities) {
        AuthenticatedUser principal =
                new AuthenticatedUser(userId, "e@x", "staff", false, UUID.randomUUID());
        return new UsernamePasswordAuthenticationToken(principal, "c", grants(authorities));
    }

    private static List<GrantedAuthority> grants(String... authorities) {
        return java.util.Arrays.stream(authorities)
                .map(SimpleGrantedAuthority::new)
                .map(a -> (GrantedAuthority) a)
                .toList();
    }

    static class HasOwner {
        private final UUID ownerUserId;

        HasOwner(UUID ownerUserId) {
            this.ownerUserId = ownerUserId;
        }

        public UUID getOwnerUserId() {
            return ownerUserId;
        }
    }
}
