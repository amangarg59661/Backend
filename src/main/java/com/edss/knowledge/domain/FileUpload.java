package com.edss.knowledge.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * In-flight presigned upload session. When the browser finishes the PUT to
 * the presigned URL it calls {@code POST /files/{upload_id}/complete} which
 * promotes this row into a permanent {@link FileRecord} in the same
 * transaction. Uncompleted rows get reaped after {@code expires_at}.
 */
@Entity
@Table(schema = "knowledge", name = "file_uploads")
public class FileUpload {

    @Id private UUID id;

    @Column(name = "upload_id")
    private String uploadId;

    @Column(name = "owner_user_id")
    private UUID ownerUserId;

    private String bucket;

    @Column(name = "storage_key")
    private String storageKey;

    @Column(name = "presigned_url")
    private String presignedUrl;

    @Column(name = "content_type")
    private String contentType;

    private String kind;

    @Column(name = "project_id")
    private UUID projectId;

    @Column(name = "milestone_id")
    private UUID milestoneId;

    @Column(name = "original_name")
    private String originalName;

    @Column(name = "expected_size")
    private Long expectedSize;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected FileUpload() {}

    public FileUpload(
            UUID id,
            String uploadId,
            UUID ownerUserId,
            String bucket,
            String storageKey,
            String presignedUrl,
            String contentType,
            FileKind kind,
            UUID projectId,
            UUID milestoneId,
            String originalName,
            Long expectedSize,
            Instant createdAt,
            Instant expiresAt) {
        this.id = id;
        this.uploadId = uploadId;
        this.ownerUserId = ownerUserId;
        this.bucket = bucket;
        this.storageKey = storageKey;
        this.presignedUrl = presignedUrl;
        this.contentType = contentType;
        this.kind = kind.wire();
        this.projectId = projectId;
        this.milestoneId = milestoneId;
        this.originalName = originalName;
        this.expectedSize = expectedSize;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public UUID getId() {
        return id;
    }

    public String getUploadId() {
        return uploadId;
    }

    public UUID getOwnerUserId() {
        return ownerUserId;
    }

    public String getBucket() {
        return bucket;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getPresignedUrl() {
        return presignedUrl;
    }

    public String getContentType() {
        return contentType;
    }

    public FileKind getKind() {
        return FileKind.ofWire(kind);
    }

    public UUID getProjectId() {
        return projectId;
    }

    public UUID getMilestoneId() {
        return milestoneId;
    }

    public String getOriginalName() {
        return originalName;
    }

    public Long getExpectedSize() {
        return expectedSize;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public boolean isCompleted() {
        return completedAt != null;
    }

    public void markCompleted(Instant at) {
        this.completedAt = at;
    }
}
