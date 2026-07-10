package com.edss.identity.domain;

import java.util.Arrays;
import java.util.Optional;

/**
 * Concrete role identifiers. {@code primary_role} on {@link User} is one of
 * these string values. Sub-roles under {@code staff} carry the same top-level
 * role but different permission sets — the distinction is enforced only via
 * granted permissions, so we do not model sub-roles as separate DB enum
 * values.
 */
public enum Role {
    CLIENT("client"),
    STAFF("staff"),
    ADMIN("admin"),
    PROJECT_MANAGER("project_manager"),
    SUPPORT_AGENT("support_agent"),
    ACCOUNTANT("accountant");

    private final String wire;

    Role(String wire) {
        this.wire = wire;
    }

    public String wire() {
        return wire;
    }

    public static Optional<Role> ofWire(String value) {
        return Arrays.stream(values()).filter(r -> r.wire.equals(value)).findFirst();
    }
}
