package com.edss.projects.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "projects", name = "contracts")
public class Contract {

    public enum Kind {
        UNSIGNED("unsigned"),
        SIGNED("signed");

        private final String wire;

        Kind(String wire) {
            this.wire = wire;
        }

        public String wire() {
            return wire;
        }

        public static Kind ofWire(String v) {
            for (Kind k : values()) {
                if (k.wire.equals(v)) return k;
            }
            throw new IllegalArgumentException("Unknown contract kind: " + v);
        }
    }

    @Id private UUID id;

    @Column(name = "project_id")
    private UUID projectId;

    private String kind;

    @Column(name = "storage_key")
    private String storageKey;

    private String sha256;

    @Column(name = "uploaded_by_user_id")
    private UUID uploadedByUserId;

    @Column(name = "uploaded_at", updatable = false)
    private Instant uploadedAt;

    protected Contract() {}

    public Contract(
            UUID id,
            UUID projectId,
            Kind kind,
            String storageKey,
            String sha256,
            UUID uploadedByUserId,
            Instant uploadedAt) {
        this.id = id;
        this.projectId = projectId;
        this.kind = kind.wire();
        this.storageKey = storageKey;
        this.sha256 = sha256;
        this.uploadedByUserId = uploadedByUserId;
        this.uploadedAt = uploadedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public Kind getKind() {
        return Kind.ofWire(kind);
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getSha256() {
        return sha256;
    }

    public UUID getUploadedByUserId() {
        return uploadedByUserId;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }
}
