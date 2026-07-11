package com.edss.knowledge.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "knowledge", name = "files")
public class FileRecord {

    @Id private UUID id;

    @Column(name = "owner_user_id")
    private UUID ownerUserId;

    private String name;

    @Column(name = "size_bytes")
    private long sizeBytes;

    @Column(name = "mime_type")
    private String mimeType;

    @Column(name = "storage_key")
    private String storageKey;

    private String bucket;

    @Column(name = "project_id")
    private UUID projectId;

    @Column(name = "milestone_id")
    private UUID milestoneId;

    private String kind;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    protected FileRecord() {}

    public FileRecord(
            UUID id,
            UUID ownerUserId,
            String name,
            long sizeBytes,
            String mimeType,
            String storageKey,
            String bucket,
            FileKind kind,
            UUID projectId,
            UUID milestoneId,
            Instant createdAt) {
        this.id = id;
        this.ownerUserId = ownerUserId;
        this.name = name;
        this.sizeBytes = sizeBytes;
        this.mimeType = mimeType;
        this.storageKey = storageKey;
        this.bucket = bucket;
        this.kind = kind.wire();
        this.projectId = projectId;
        this.milestoneId = milestoneId;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerUserId() {
        return ownerUserId;
    }

    public String getName() {
        return name;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getBucket() {
        return bucket;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public UUID getMilestoneId() {
        return milestoneId;
    }

    public FileKind getKind() {
        return FileKind.ofWire(kind);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
