package com.edss.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "identity", name = "users")
public class User {

    @Id private UUID id;

    private String email;

    private String name;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "primary_role")
    private String primaryRole;

    @Column(name = "has_both_roles")
    private boolean hasBothRoles;

    private boolean active;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected User() {}

    public User(
            UUID id,
            String email,
            String name,
            String passwordHash,
            String primaryRole,
            boolean hasBothRoles,
            boolean active,
            Instant createdAt) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.passwordHash = passwordHash;
        this.primaryRole = primaryRole;
        this.hasBothRoles = hasBothRoles;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void changePasswordHash(String newHash, Instant at) {
        this.passwordHash = newHash;
        this.updatedAt = at;
    }

    public String getPrimaryRole() {
        return primaryRole;
    }

    public boolean isHasBothRoles() {
        return hasBothRoles;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
