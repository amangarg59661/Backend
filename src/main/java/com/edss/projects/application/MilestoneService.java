package com.edss.projects.application;

import com.edss.projects.domain.Milestone;
import com.edss.projects.domain.MilestoneReview;
import com.edss.projects.domain.ReviewVerdict;
import com.edss.projects.domain.events.ProjectEvents;
import com.edss.projects.infrastructure.MilestoneRepository;
import com.edss.projects.infrastructure.MilestoneReviewRepository;
import com.edss.shared.api.ApiErrorCode;
import com.edss.shared.api.ApiException;
import com.edss.shared.events.OutboxWriter;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MilestoneService {

    private final MilestoneRepository milestones;
    private final MilestoneReviewRepository reviews;
    private final OutboxWriter outbox;
    private final Clock clock;

    public MilestoneService(
            MilestoneRepository milestones,
            MilestoneReviewRepository reviews,
            OutboxWriter outbox,
            Clock clock) {
        this.milestones = milestones;
        this.reviews = reviews;
        this.outbox = outbox;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<Milestone> listForProject(UUID projectId) {
        return milestones.findByProjectIdOrderByOrdinalAsc(projectId);
    }

    public Milestone submit(UUID projectId, UUID milestoneId, UUID actorUserId) {
        Milestone milestone = fetch(projectId, milestoneId);
        Instant now = clock.instant();
        milestone.submit(now);
        outbox.append(
                "projects",
                new ProjectEvents.MilestoneSubmitted(
                        UUID.randomUUID(), now, projectId, milestoneId, milestone.getOrdinal()),
                Map.of(
                        "project_id", projectId,
                        "milestone_id", milestoneId,
                        "ordinal", milestone.getOrdinal(),
                        "submitted_by", actorUserId));
        return milestone;
    }

    public MilestoneReview review(
            UUID projectId,
            UUID milestoneId,
            ReviewVerdict verdict,
            String comment,
            UUID reviewerUserId) {
        Milestone milestone = fetch(projectId, milestoneId);
        Instant now = clock.instant();
        milestone.applyReview(verdict, now);
        MilestoneReview review =
                new MilestoneReview(
                        UUID.randomUUID(), milestoneId, verdict, comment, reviewerUserId, now);
        reviews.save(review);
        outbox.append(
                "projects",
                new ProjectEvents.MilestoneReviewed(
                        UUID.randomUUID(),
                        now,
                        projectId,
                        milestoneId,
                        verdict.wire(),
                        reviewerUserId),
                Map.of(
                        "project_id", projectId,
                        "milestone_id", milestoneId,
                        "verdict", verdict.wire(),
                        "reviewer_user_id", reviewerUserId));
        return review;
    }

    @Transactional(readOnly = true)
    public List<MilestoneReview> reviewsFor(UUID milestoneId) {
        return reviews.findByMilestoneIdOrderByReviewedAtDesc(milestoneId);
    }

    private Milestone fetch(UUID projectId, UUID milestoneId) {
        Milestone milestone =
                milestones.findById(milestoneId)
                        .orElseThrow(
                                () ->
                                        new ApiException(
                                                ApiErrorCode.NOT_FOUND, "Milestone not found."));
        if (!milestone.getProjectId().equals(projectId)) {
            throw new ApiException(ApiErrorCode.NOT_FOUND, "Milestone not found.");
        }
        return milestone;
    }
}
