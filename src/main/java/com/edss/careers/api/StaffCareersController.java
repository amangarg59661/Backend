package com.edss.careers.api;

import com.edss.careers.api.dto.JobApplicationDto;
import com.edss.careers.api.dto.JobApplicationReviewRequest;
import com.edss.careers.api.dto.JobPostingCreateRequest;
import com.edss.careers.api.dto.JobPostingDto;
import com.edss.careers.api.dto.JobPostingUpdateRequest;
import com.edss.careers.application.JobApplicationService;
import com.edss.careers.application.JobPostingService;
import com.edss.careers.domain.JobApplication;
import com.edss.careers.domain.JobPosting;
import com.edss.shared.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Staff careers surface. All endpoints require {@code careers:*}
 * permission. Admin roles seeded with {@code careers:*} by
 * PermissionCatalog; project managers get read-only access to postings
 * and applications.
 */
@RestController
@RequestMapping("/api/v1/staff/careers")
@Tag(name = "careers-staff", description = "Careers moderation surface (staff only).")
public class StaffCareersController {

    private final JobPostingService postings;
    private final JobApplicationService applications;

    public StaffCareersController(
            JobPostingService postings, JobApplicationService applications) {
        this.postings = postings;
        this.applications = applications;
    }

    // -------------------------------------------------------------------
    // Postings
    // -------------------------------------------------------------------

    @GetMapping
    @PreAuthorize("hasAuthority('careers:read') or hasAuthority('careers:*') or hasAuthority('admin:*')")
    public List<JobPostingDto> list(@RequestParam(defaultValue = "100") int limit) {
        return postings.listAll(limit).stream()
                .map(StaffCareersController::toStaffDto)
                .toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('careers:read') or hasAuthority('careers:*') or hasAuthority('admin:*')")
    public JobPostingDto fetch(@PathVariable UUID id) {
        return toStaffDto(postings.fetch(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('careers:write') or hasAuthority('careers:*') or hasAuthority('admin:*')")
    public JobPostingDto create(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody JobPostingCreateRequest req) {
        JobPostingService.NewPosting spec =
                new JobPostingService.NewPosting(
                        req.slug(),
                        req.title(),
                        req.team(),
                        req.location(),
                        req.employmentType(),
                        req.commitment(),
                        req.summary(),
                        req.responsibilities(),
                        req.requirements(),
                        req.salaryRangeMin(),
                        req.salaryRangeMax(),
                        req.currency());
        return toStaffDto(postings.create(spec, principal.userId()));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('careers:write') or hasAuthority('careers:*') or hasAuthority('admin:*')")
    public JobPostingDto update(
            @PathVariable UUID id, @Valid @RequestBody JobPostingUpdateRequest req) {
        JobPostingService.EditPosting spec =
                new JobPostingService.EditPosting(
                        req.title(),
                        req.team(),
                        req.location(),
                        req.employmentType(),
                        req.commitment(),
                        req.summary(),
                        req.responsibilities(),
                        req.requirements(),
                        req.salaryRangeMin(),
                        req.salaryRangeMax(),
                        req.currency());
        return toStaffDto(postings.update(id, spec));
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('careers:write') or hasAuthority('careers:*') or hasAuthority('admin:*')")
    public JobPostingDto publish(@PathVariable UUID id) {
        return toStaffDto(postings.publish(id));
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAuthority('careers:write') or hasAuthority('careers:*') or hasAuthority('admin:*')")
    public JobPostingDto archive(@PathVariable UUID id) {
        return toStaffDto(postings.archive(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('careers:write') or hasAuthority('careers:*') or hasAuthority('admin:*')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        postings.delete(id);
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------
    // Applications
    // -------------------------------------------------------------------

    @GetMapping("/{id}/applications")
    @PreAuthorize(
            "hasAuthority('careers:applications:read') or hasAuthority('careers:*') or hasAuthority('admin:*')")
    public List<JobApplicationDto> listApplications(
            @PathVariable UUID id, @RequestParam(defaultValue = "200") int limit) {
        return applications.listForPosting(id, limit).stream()
                .map(StaffCareersController::toStaffDto)
                .toList();
    }

    @PatchMapping("/applications/{applicationId}")
    @PreAuthorize(
            "hasAuthority('careers:applications:write') or hasAuthority('careers:*') or hasAuthority('admin:*')")
    public JobApplicationDto review(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID applicationId,
            @Valid @RequestBody JobApplicationReviewRequest req) {
        return toStaffDto(
                applications.review(applicationId, req.status(), req.note(), principal.userId()));
    }

    static JobPostingDto toStaffDto(JobPosting p) {
        return new JobPostingDto(
                p.getId(),
                p.getSlug(),
                p.getTitle(),
                p.getTeam(),
                p.getLocation(),
                p.getEmploymentType(),
                p.getCommitment(),
                p.getSummary(),
                p.getResponsibilities(),
                p.getRequirements(),
                p.getSalaryRangeMin(),
                p.getSalaryRangeMax(),
                p.getCurrency(),
                p.getStatus().wire(),
                p.getPostedAt(),
                p.getPublishedAt(),
                p.getArchivedAt(),
                p.getCreatedAt(),
                p.getUpdatedAt());
    }

    static JobApplicationDto toStaffDto(JobApplication a) {
        return new JobApplicationDto(
                a.getId(),
                a.getJobPostingId(),
                a.getApplicantName(),
                a.getApplicantEmail(),
                a.getApplicantPhone(),
                a.getResumeUrl(),
                a.getCoverLetter(),
                a.getStatus().wire(),
                a.getReviewerNote(),
                a.getSubmittedAt(),
                a.getReviewedAt(),
                a.getReviewedByUserId());
    }
}
