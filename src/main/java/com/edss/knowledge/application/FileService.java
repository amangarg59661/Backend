package com.edss.knowledge.application;

import com.edss.integrations.storage.StorageClient;
import com.edss.knowledge.domain.FileKind;
import com.edss.knowledge.domain.FileRecord;
import com.edss.knowledge.domain.FileUpload;
import com.edss.knowledge.domain.events.KnowledgeEvents;
import com.edss.knowledge.infrastructure.FileRepository;
import com.edss.knowledge.infrastructure.FileUploadRepository;
import com.edss.shared.api.ApiErrorCode;
import com.edss.shared.api.ApiException;
import com.edss.shared.config.StorageProperties;
import com.edss.shared.events.OutboxWriter;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class FileService {

    private static final Logger log = LoggerFactory.getLogger(FileService.class);
    private static final Duration UPLOAD_TTL = Duration.ofMinutes(15);
    private static final Duration DOWNLOAD_TTL = Duration.ofMinutes(5);

    private final FileRepository files;
    private final FileUploadRepository uploads;
    private final StorageClient storage;
    private final StorageProperties.Buckets buckets;
    private final OutboxWriter outbox;
    private final Clock clock;

    public FileService(
            FileRepository files,
            FileUploadRepository uploads,
            StorageClient storage,
            StorageProperties storageProperties,
            OutboxWriter outbox,
            Clock clock) {
        this.files = files;
        this.uploads = uploads;
        this.storage = storage;
        this.buckets = storageProperties.buckets();
        this.outbox = outbox;
        this.clock = clock;
    }

    public PresignedUpload presignUpload(
            UUID ownerUserId,
            String originalName,
            String contentType,
            long expectedSize,
            FileKind kind,
            UUID projectId,
            UUID milestoneId) {
        String bucket = bucketFor(kind);
        String key = ownerUserId + "/" + UUID.randomUUID() + "-" + sanitize(originalName);
        StorageClient.Presigned presigned = storage.presignUpload(bucket, key, contentType, UPLOAD_TTL);
        String uploadId = "upl_" + UUID.randomUUID();
        Instant now = clock.instant();
        FileUpload row =
                new FileUpload(
                        UUID.randomUUID(),
                        uploadId,
                        ownerUserId,
                        bucket,
                        key,
                        presigned.url().toString(),
                        contentType,
                        kind,
                        projectId,
                        milestoneId,
                        originalName,
                        expectedSize,
                        now,
                        presigned.expiresAt());
        uploads.save(row);
        return new PresignedUpload(uploadId, presigned.url(), presigned.expiresAt());
    }

    public FileRecord complete(String uploadId, UUID actorUserId, long actualSize) {
        FileUpload session =
                uploads.findByUploadId(uploadId)
                        .orElseThrow(
                                () ->
                                        new ApiException(
                                                ApiErrorCode.NOT_FOUND, "Upload session unknown."));
        if (!session.getOwnerUserId().equals(actorUserId)) {
            throw new ApiException(ApiErrorCode.FORBIDDEN, "Not your upload.");
        }
        Instant now = clock.instant();
        if (session.isCompleted()) {
            throw new ApiException(ApiErrorCode.VALIDATION_FAILED, "Upload already completed.");
        }
        if (session.getExpiresAt().isBefore(now)) {
            throw new ApiException(ApiErrorCode.VALIDATION_FAILED, "Upload session expired.");
        }
        session.markCompleted(now);
        FileRecord file =
                new FileRecord(
                        UUID.randomUUID(),
                        session.getOwnerUserId(),
                        session.getOriginalName(),
                        actualSize,
                        session.getContentType(),
                        session.getStorageKey(),
                        session.getBucket(),
                        session.getKind(),
                        session.getProjectId(),
                        session.getMilestoneId(),
                        now);
        files.save(file);
        outbox.append(
                "knowledge",
                new KnowledgeEvents.FileUploaded(
                        UUID.randomUUID(),
                        now,
                        file.getId(),
                        file.getOwnerUserId(),
                        file.getKind().wire(),
                        file.getProjectId(),
                        file.getMilestoneId(),
                        actualSize),
                Map.of(
                        "file_id", file.getId(),
                        "owner_user_id", file.getOwnerUserId(),
                        "kind", file.getKind().wire(),
                        "size_bytes", actualSize,
                        "project_id",
                        file.getProjectId() == null ? "" : file.getProjectId().toString(),
                        "milestone_id",
                        file.getMilestoneId() == null ? "" : file.getMilestoneId().toString()));
        log.info("File {} completed by {}", file.getId(), actorUserId);
        return file;
    }

    @Transactional(readOnly = true)
    public URI downloadUrl(UUID fileId, UUID actorUserId, boolean isStaff) {
        FileRecord file =
                files.findById(fileId)
                        .orElseThrow(
                                () -> new ApiException(ApiErrorCode.NOT_FOUND, "File not found."));
        if (!isStaff && !file.getOwnerUserId().equals(actorUserId)) {
            throw new ApiException(ApiErrorCode.FORBIDDEN, "Not your file.");
        }
        return storage.presignDownload(file.getBucket(), file.getStorageKey(), DOWNLOAD_TTL);
    }

    @Transactional(readOnly = true)
    public List<FileRecord> list(
            UUID actorUserId,
            boolean isStaff,
            UUID projectId,
            UUID milestoneId,
            int limit) {
        Limit lim = Limit.of(Math.max(1, Math.min(200, limit)));
        if (milestoneId != null) {
            return files.findByMilestoneIdOrderByCreatedAtDesc(milestoneId, lim);
        }
        if (projectId != null) {
            return files.findByProjectIdOrderByCreatedAtDesc(projectId, lim);
        }
        return isStaff
                ? files.findAllByOrderByCreatedAtDesc(lim)
                : files.findByOwnerUserIdOrderByCreatedAtDesc(actorUserId, lim);
    }

    private String bucketFor(FileKind kind) {
        return switch (kind) {
            case CONTRACT -> buckets.contracts();
            case MILESTONE_DELIVERABLE -> buckets.milestones();
            case AVATAR -> buckets.avatars();
            case GENERAL, PROJECT_ASSET -> buckets.files();
        };
    }

    private static String sanitize(String name) {
        if (name == null) {
            return "file";
        }
        return name.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    public record PresignedUpload(String uploadId, URI presignedUrl, Instant expiresAt) {}
}
