package com.edss.identity.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.edss.identity.domain.Role;
import org.junit.jupiter.api.Test;

class PermissionCatalogTest {

    private final PermissionCatalog catalog = new PermissionCatalog();

    @Test
    void adminHasWildcard() {
        assertThat(catalog.permissionsFor(Role.ADMIN)).contains("admin:*");
    }

    @Test
    void clientPermissionsAllScopedOwn() {
        for (String p : catalog.permissionsFor(Role.CLIENT)) {
            assertThat(p).endsWith(":own");
        }
    }

    @Test
    void projectManagerCoversProjectsAndInquiries() {
        var perms = catalog.permissionsFor(Role.PROJECT_MANAGER);
        assertThat(perms).contains("projects:*");
        assertThat(perms).contains("relationship:inquiry:*");
    }

    @Test
    void accountantCoversFinance() {
        var perms = catalog.permissionsFor(Role.ACCOUNTANT);
        assertThat(perms).contains("finance:*");
    }

    @Test
    void supportAgentCoversTickets() {
        var perms = catalog.permissionsFor(Role.SUPPORT_AGENT);
        assertThat(perms).contains("commitments:ticket:*");
    }

    @Test
    void unknownRoleReturnsEmpty() {
        assertThat(catalog.permissionsForWire("bogus")).isEmpty();
    }
}
