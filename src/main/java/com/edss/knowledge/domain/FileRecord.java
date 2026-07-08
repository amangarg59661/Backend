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

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    protected FileRecord() {}

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

    public Instant getCreatedAt() {
        return createdAt;
    }
}
