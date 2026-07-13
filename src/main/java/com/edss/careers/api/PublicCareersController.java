package com.edss.careers.api;

import com.edss.careers.api.dto.JobApplicationDto;
import com.edss.careers.api.dto.JobApplicationSubmitRequest;
import com.edss.careers.api.dto.JobPostingDto;
import com.edss.careers.application.JobApplicationService;
import com.edss.careers.application.JobPostingService;
import com.edss.careers.domain.JobApplication;
import com.edss.careers.domain.JobPosting;
import com.edss.shared.api.HttpRequests;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public careers surface — no authentication. Only {@code published}
 * postings are visible. Marketing site consumes GET endpoints for the
 * careers page and POSTs applications for the apply form.
 */
@RestController
@RequestMapping("/api/v1/careers")
@Tag(name = "careers-public", description = "Public job postings + application intake.")
public class PublicCareersController {

    private final JobPostingService postings;
    private final JobApplicationService applications;

    public PublicCareersController(
            JobPostingService postings, JobApplicationService applications) {
        this.postings = postings;
        this.applications = applications;
    }

    @GetMapping
    public List<JobPostingDto> list(@RequestParam(defaultValue = "100") int limit) {
        return postings.listPublished(limit).stream()
                .map(PublicCareersController::toPublicDto)
                .toList();
    }

    @GetMapping("/{slug}")
    public JobPostingDto fetch(@PathVariable String slug) {
        return toPublicDto(postings.fetchBySlug(slug));
    }

    @PostMapping("/{slug}/apply")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public JobApplicationDto apply(
            @PathVariable String slug,
            @Valid @RequestBody JobApplicationSubmitRequest req,
            HttpServletRequest http) {
        JobApplication row =
                applications.submit(
                        slug,
                        req.applicantName(),
                        req.applicantEmail(),
                        req.applicantPhone(),
                        req.resumeUrl(),
                        req.coverLetter(),
                        HttpRequests.clientIp(http));
        // Public response omits reviewer + note; those are staff-only fields.
        return new JobApplicationDto(
                row.getId(),
                row.getJobPostingId(),
                row.getApplicantName(),
                row.getApplicantEmail(),
                null,
                null,
                null,
                row.getStatus().wire(),
                null,
                row.getSubmittedAt(),
                null,
                null);
    }

    static JobPostingDto toPublicDto(JobPosting p) {
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
                null,
                null,
                null);
    }
}
