package com.edss.careers.application;

import com.edss.careers.domain.JobApplication;
import com.edss.careers.domain.JobApplicationStatus;
import com.edss.careers.domain.JobPosting;
import com.edss.careers.domain.JobPostingStatus;
import com.edss.careers.domain.events.CareersEvents;
import com.edss.careers.infrastructure.JobApplicationRepository;
import com.edss.shared.api.ApiErrorCode;
import com.edss.shared.api.ApiException;
import com.edss.shared.events.OutboxWriter;
import com.edss.shared.ratelimit.RateLimitDecision;
import com.edss.shared.ratelimit.RateLimiter;
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

/**
 * Public application intake + staff review flow.
 *
 * <p>Public {@link #submit} is rate-limited on source IP (5 / hour) to
 * blunt scripted apply-bombs, and also caps duplicate applications from
 * the same email + posting pair to 3 total (a legitimate re-apply is
 * possible; a spammer hitting the same slug repeatedly is not).</p>
 */
@Service
@Transactional
public class JobApplicationService {

    private static final Logger log = LoggerFactory.getLogger(JobApplicationService.class);
    private static final Duration RATE_WINDOW = Duration.ofHours(1);
    private static final int RATE_LIMIT_PER_IP = 5;
    private static final long MAX_APPLICATIONS_PER_EMAIL = 3;

    private final JobApplicationRepository applications;
    private final JobPostingService postings;
    private final OutboxWriter outbox;
    private final RateLimiter rateLimiter;
    private final Clock clock;

    public JobApplicationService(
            JobApplicationRepository applications,
            JobPostingService postings,
            OutboxWriter outbox,
            RateLimiter rateLimiter,
            Clock clock) {
        this.applications = applications;
        this.postings = postings;
        this.outbox = outbox;
        this.rateLimiter = rateLimiter;
        this.clock = clock;
    }

    public JobApplication submit(
            String slug,
            String applicantName,
            String applicantEmail,
            String applicantPhone,
            String resumeUrl,
            String coverLetter,
            String sourceIp) {
        enforceRateLimit(sourceIp);
        JobPosting posting = postings.fetchBySlug(slug);
        if (posting.getStatus() != JobPostingStatus.PUBLISHED) {
            throw new ApiException(
                    ApiErrorCode.NOT_FOUND, "Job posting is no longer accepting applications.");
        }
        long existing =
                applications.countByJobPostingIdAndApplicantEmail(posting.getId(), applicantEmail);
        if (existing >= MAX_APPLICATIONS_PER_EMAIL) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_FAILED,
                    "You have already applied for this role.");
        }
        Instant now = clock.instant();
        JobApplication row =
                new JobApplication(
                        UUID.randomUUID(),
                        posting.getId(),
                        applicantName,
                        applicantEmail,
                        applicantPhone,
                        resumeUrl,
                        coverLetter,
                        sourceIp,
                        now);
        applications.save(row);
        outbox.append(
                "careers",
                new CareersEvents.ApplicationSubmitted(
                        UUID.randomUUID(),
                        now,
                        row.getId(),
                        posting.getId(),
                        applicantEmail,
                        applicantName,
                        posting.getTitle()),
                Map.of(
                        "application_id", row.getId(),
                        "posting_id", posting.getId(),
                        "email", applicantEmail,
                        "name", applicantName,
                        "posting_title", posting.getTitle()));
        log.info(
                "Job application {} for posting {} from {}",
                row.getId(),
                posting.getSlug(),
                applicantEmail);
        return row;
    }

    public JobApplication review(
            UUID applicationId, String targetStatus, String note, UUID reviewerUserId) {
        JobApplication row =
                applications.findById(applicationId)
                        .orElseThrow(
                                () -> new ApiException(
                                        ApiErrorCode.NOT_FOUND, "Application not found."));
        JobApplicationStatus target;
        try {
            target = JobApplicationStatus.ofWire(targetStatus);
        } catch (IllegalArgumentException ex) {
            throw new ApiException(ApiErrorCode.VALIDATION_FAILED, "Unknown application status.");
        }
        Instant now = clock.instant();
        row.review(target, note, reviewerUserId, now);
        JobPosting posting = postings.fetch(row.getJobPostingId());
        outbox.append(
                "careers",
                new CareersEvents.ApplicationReviewed(
                        UUID.randomUUID(),
                        now,
                        row.getId(),
                        posting.getId(),
                        row.getApplicantEmail(),
                        row.getApplicantName(),
                        target.wire(),
                        posting.getTitle()),
                Map.of(
                        "application_id", row.getId(),
                        "posting_id", posting.getId(),
                        "email", row.getApplicantEmail(),
                        "name", row.getApplicantName(),
                        "status", target.wire(),
                        "posting_title", posting.getTitle()));
        return row;
    }

    @Transactional(readOnly = true)
    public List<JobApplication> listForPosting(UUID postingId, int limit) {
        return applications.findByJobPostingIdOrderBySubmittedAtDesc(
                postingId, Limit.of(Math.max(1, Math.min(200, limit))));
    }

    private void enforceRateLimit(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return;
        }
        RateLimitDecision decision =
                rateLimiter.hit("careers:apply:ip:" + ipAddress, RATE_LIMIT_PER_IP, RATE_WINDOW);
        if (!decision.allowed()) {
            throw new ApiException(
                    ApiErrorCode.RATE_LIMITED,
                    "Too many applications from this source. Try again later.",
                    Map.of("retry_after", decision.retryAfter().getSeconds()));
        }
    }
}
