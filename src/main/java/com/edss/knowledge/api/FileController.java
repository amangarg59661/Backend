package com.edss.knowledge.api;

import com.edss.knowledge.api.dto.CompleteUploadRequest;
import com.edss.knowledge.api.dto.DownloadUrlResponse;
import com.edss.knowledge.api.dto.FileDto;
import com.edss.knowledge.api.dto.PresignUploadRequest;
import com.edss.knowledge.api.dto.PresignUploadResponse;
import com.edss.knowledge.application.FileService;
import com.edss.knowledge.domain.FileKind;
import com.edss.knowledge.domain.FileRecord;
import com.edss.shared.api.PaginatedResponse;
import com.edss.shared.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/files")
@PreAuthorize("isAuthenticated()")
@Tag(name = "files", description = "Uploaded files backed by Supabase or S3.")
public class FileController {

    private final FileService files;

    public FileController(FileService files) {
        this.files = files;
    }

    @PostMapping("/presign")
    public PresignUploadResponse presign(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody PresignUploadRequest req) {
        FileService.PresignedUpload result =
                files.presignUpload(
                        principal.userId(),
                        req.name(),
                        req.contentType(),
                        req.sizeBytes(),
                        FileKind.ofWire(req.kind()),
                        req.projectId(),
                        req.milestoneId());
        return new PresignUploadResponse(
                result.uploadId(), result.presignedUrl(), result.expiresAt());
    }

    @PostMapping("/{uploadId}/complete")
    public FileDto complete(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable String uploadId,
            @Valid @RequestBody CompleteUploadRequest req) {
        return toDto(files.complete(uploadId, principal.userId(), req.actualSize()));
    }

    @GetMapping
    public PaginatedResponse<FileDto> list(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(required = false) UUID projectId,
            @RequestParam(required = false) UUID milestoneId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "50") int limit) {
        boolean isStaff = isStaff(principal);
        List<FileDto> items =
                files.list(principal.userId(), isStaff, projectId, milestoneId, limit).stream()
                        .map(FileController::toDto)
                        .toList();
        return new PaginatedResponse<>(items, null, false);
    }

    @GetMapping("/{fileId}/download-url")
    public DownloadUrlResponse downloadUrl(
            @AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID fileId) {
        return new DownloadUrlResponse(
                files.downloadUrl(fileId, principal.userId(), isStaff(principal)));
    }

    private static boolean isStaff(AuthenticatedUser principal) {
        return "staff".equals(principal.primaryRole()) || principal.hasBothRoles();
    }

    private static FileDto toDto(FileRecord f) {
        return new FileDto(
                f.getId(),
                f.getOwnerUserId(),
                f.getName(),
                f.getSizeBytes(),
                f.getMimeType(),
                f.getKind().wire(),
                f.getProjectId(),
                f.getMilestoneId(),
                f.getCreatedAt());
    }
}
