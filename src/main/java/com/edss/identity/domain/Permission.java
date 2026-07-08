package com.edss.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(schema = "identity", name = "user_permissions")
public class Permission {

    @EmbeddedId private PermissionId id;

    protected Permission() {}

    public Permission(UUID userId, String permission) {
        this.id = new PermissionId(userId, permission);
    }

    public UUID getUserId() {
        return id.userId;
    }

    public String getPermission() {
        return id.permission;
    }

    @jakarta.persistence.Embeddable
    public static class PermissionId implements Serializable {
        @Column(name = "user_id")
        private UUID userId;

        @Column(name = "permission")
        private String permission;

        protected PermissionId() {}

        public PermissionId(UUID userId, String permission) {
            this.userId = userId;
            this.permission = permission;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PermissionId that)) return false;
            return Objects.equals(userId, that.userId)
                    && Objects.equals(permission, that.permission);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, permission);
        }
    }
}
