package com.edss.careers.application;

import com.edss.careers.domain.JobPosting;
import com.edss.careers.domain.JobPostingStatus;
import com.edss.careers.domain.events.CareersEvents;
import com.edss.careers.infrastructure.JobPostingRepository;
import com.edss.shared.api.ApiErrorCode;
import com.edss.shared.api.ApiException;
import com.edss.shared.events.OutboxWriter;
import java.time.Clock;
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
 * Job posting lifecycle:
 * <pre>
 *   draft ── publish() ─→ published ── archive() ─→ archived
 *              │                                       ▲
 *              └── archive() ──────────────────────────┘
 * </pre>
 * Public GET surface only returns {@code published} postings; staff can
 * see all statuses.
 */
@Service
@Transactional
public class JobPostingService {

    private static final Logger log = LoggerFactory.getLogger(JobPostingService.class);

    private final JobPostingRepository postings;
    private final OutboxWriter outbox;
    private final Clock clock;

    public JobPostingService(
            JobPostingRepository postings, OutboxWriter outbox, Clock clock) {
        this.postings = postings;
        this.outbox = outbox;
        this.clock = clock;
    }

    public JobPosting create(NewPosting spec, UUID createdByUserId) {
        postings.findBySlug(spec.slug())
                .ifPresent(existing -> {
                    throw new ApiException(
                            ApiErrorCode.VALIDATION_FAILED,
                            "Slug already in use: " + spec.slug());
                });
        Instant now = clock.instant();
        JobPosting posting =
                new JobPosting(
                        UUID.randomUUID(),
                        spec.slug(),
                        spec.title(),
                        spec.team(),
                        spec.location(),
                        spec.employmentType(),
                        spec.commitment(),
                        spec.summary(),
                        spec.responsibilities(),
                        spec.requirements(),
                        spec.salaryRangeMin(),
                        spec.salaryRangeMax(),
                        spec.currency(),
                        createdByUserId,
                        now);
        postings.save(posting);
        log.info("Created job posting {} ({})", posting.getSlug(), posting.getId());
        return posting;
    }

    public JobPosting update(UUID id, EditPosting spec) {
        JobPosting posting = fetch(id);
        posting.editContent(
                spec.title(),
                spec.team(),
                spec.location(),
                spec.employmentType(),
                spec.commitment(),
                spec.summary(),
                spec.responsibilities(),
                spec.requirements(),
                spec.salaryRangeMin(),
                spec.salaryRangeMax(),
                spec.currency(),
                clock.instant());
        return posting;
    }

    public JobPosting publish(UUID id) {
        JobPosting posting = fetch(id);
        if (posting.getStatus() != JobPostingStatus.DRAFT) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_FAILED,
                    "Only draft postings can be published (current: " + posting.getStatus().wire() + ").");
        }
        Instant now = clock.instant();
        posting.publish(now);
        outbox.append(
                "careers",
                new CareersEvents.JobPostingPublished(
                        UUID.randomUUID(), now, posting.getId(), posting.getSlug(), posting.getTitle()),
                Map.of(
                        "posting_id", posting.getId(),
                        "slug", posting.getSlug(),
                        "title", posting.getTitle()));
        return posting;
    }

    public JobPosting archive(UUID id) {
        JobPosting posting = fetch(id);
        if (posting.getStatus() == JobPostingStatus.ARCHIVED) {
            return posting;
        }
        posting.archive(clock.instant());
        return posting;
    }

    public void delete(UUID id) {
        JobPosting posting = fetch(id);
        if (posting.getStatus() != JobPostingStatus.DRAFT) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_FAILED,
                    "Only draft postings can be deleted; archive published postings instead.");
        }
        postings.delete(posting);
    }

    @Transactional(readOnly = true)
    public List<JobPosting> listPublished(int limit) {
        return postings.findByStatusOrderByPublishedAtDesc(
                JobPostingStatus.PUBLISHED.wire(), Limit.of(cap(limit)));
    }

    @Transactional(readOnly = true)
    public List<JobPosting> listAll(int limit) {
        return postings.findAllByOrderByUpdatedAtDesc(Limit.of(cap(limit)));
    }

    @Transactional(readOnly = true)
    public JobPosting fetch(UUID id) {
        return postings.findById(id)
                .orElseThrow(
                        () -> new ApiException(ApiErrorCode.NOT_FOUND, "Job posting not found."));
    }

    @Transactional(readOnly = true)
    public JobPosting fetchBySlug(String slug) {
        return postings.findBySlug(slug)
                .orElseThrow(
                        () -> new ApiException(ApiErrorCode.NOT_FOUND, "Job posting not found."));
    }

    private static int cap(int limit) {
        return Math.max(1, Math.min(200, limit));
    }

    public record NewPosting(
            String slug,
            String title,
            String team,
            String location,
            String employmentType,
            String commitment,
            String summary,
            List<String> responsibilities,
            List<String> requirements,
            Long salaryRangeMin,
            Long salaryRangeMax,
            String currency) {}

    public record EditPosting(
            String title,
            String team,
            String location,
            String employmentType,
            String commitment,
            String summary,
            List<String> responsibilities,
            List<String> requirements,
            Long salaryRangeMin,
            Long salaryRangeMax,
            String currency) {}
}
