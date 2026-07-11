package com.edss.projects.application;

import com.edss.projects.domain.BillingModel;
import com.edss.projects.domain.Contract;
import com.edss.projects.domain.Milestone;
import com.edss.projects.domain.Project;
import com.edss.projects.domain.ProjectPhase;
import com.edss.projects.domain.ProjectPhaseHistory;
import com.edss.projects.domain.events.ProjectEvents;
import com.edss.projects.infrastructure.ContractRepository;
import com.edss.projects.infrastructure.MilestoneRepository;
import com.edss.projects.infrastructure.ProjectPhaseHistoryRepository;
import com.edss.projects.infrastructure.ProjectRepository;
import com.edss.shared.api.ApiErrorCode;
import com.edss.shared.api.ApiException;
import com.edss.shared.events.OutboxWriter;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
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
public class ProjectService {

    private static final Logger log = LoggerFactory.getLogger(ProjectService.class);

    private final ProjectRepository projects;
    private final MilestoneRepository milestones;
    private final ContractRepository contracts;
    private final ProjectPhaseHistoryRepository history;
    private final OutboxWriter outbox;
    private final Clock clock;

    public ProjectService(
            ProjectRepository projects,
            MilestoneRepository milestones,
            ContractRepository contracts,
            ProjectPhaseHistoryRepository history,
            OutboxWriter outbox,
            Clock clock) {
        this.projects = projects;
        this.milestones = milestones;
        this.contracts = contracts;
        this.history = history;
        this.outbox = outbox;
        this.clock = clock;
    }

    public Project create(NewProject spec, UUID creatorUserId) {
        Instant now = clock.instant();
        UUID projectId = UUID.randomUUID();
        Project project =
                new Project(
                        projectId,
                        spec.clientUserId(),
                        spec.title(),
                        spec.description(),
                        spec.billingModel(),
                        spec.maintenanceDurationDays(),
                        spec.totalAmountMinor(),
                        spec.currency(),
                        now);
        projects.save(project);

        int ordinal = 1;
        for (NewMilestone m : spec.milestones()) {
            milestones.save(
                    new Milestone(
                            UUID.randomUUID(),
                            projectId,
                            ordinal++,
                            m.title(),
                            m.description(),
                            m.amountMinor(),
                            m.dueAt()));
        }
        history.save(
                new ProjectPhaseHistory(
                        UUID.randomUUID(),
                        projectId,
                        null,
                        ProjectPhase.DISCUSSION,
                        creatorUserId,
                        "created",
                        now));
        outbox.append(
                "projects",
                new ProjectEvents.ProjectCreated(
                        UUID.randomUUID(),
                        now,
                        projectId,
                        spec.clientUserId(),
                        spec.billingModel().wire()),
                Map.of(
                        "project_id", projectId,
                        "owner_user_id", spec.clientUserId(),
                        "billing_model", spec.billingModel().wire(),
                        "milestone_count", spec.milestones().size()));
        return project;
    }

    @Transactional(readOnly = true)
    public List<Project> list(UUID actorUserId, boolean isStaff, int limit) {
        Limit lim = Limit.of(Math.max(1, Math.min(200, limit)));
        return isStaff
                ? projects.findAllByOrderByCreatedAtDesc(lim)
                : projects.findByOwnerUserIdOrderByCreatedAtDesc(actorUserId, lim);
    }

    @Transactional(readOnly = true)
    public Project fetch(UUID projectId) {
        return projects.findById(projectId)
                .orElseThrow(
                        () -> new ApiException(ApiErrorCode.NOT_FOUND, "Project not found."));
    }

    @Transactional(readOnly = true)
    public List<ProjectPhaseHistory> phaseHistory(UUID projectId) {
        return history.findByProjectIdOrderByTransitionedAtDesc(projectId);
    }

    /** Runs guard checks then advances one step forward. */
    public Project advance(UUID projectId, UUID actorUserId, String note) {
        Project project = fetch(projectId);
        ProjectPhase current = project.getPhase();
        ProjectPhase target = ProjectPhaseTransition.next(current);
        enforceGuard(project, current, target);
        applyTransition(project, current, target, actorUserId, note == null ? "advance" : note);
        return project;
    }

    /** Admin-only backward move. Bypasses guards; audits the reason. */
    public Project reset(UUID projectId, UUID actorUserId, ProjectPhase target, String note) {
        Project project = fetch(projectId);
        if (note == null || note.isBlank()) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_FAILED, "Reset requires a non-empty note.");
        }
        applyTransition(project, project.getPhase(), target, actorUserId, note);
        return project;
    }

    private void applyTransition(
            Project project,
            ProjectPhase from,
            ProjectPhase to,
            UUID actorUserId,
            String note) {
        Instant now = clock.instant();
        project.transitionTo(to, now);
        history.save(
                new ProjectPhaseHistory(
                        UUID.randomUUID(), project.getId(), from, to, actorUserId, note, now));
        outbox.append(
                "projects",
                new ProjectEvents.PhaseTransitioned(
                        UUID.randomUUID(),
                        now,
                        project.getId(),
                        from == null ? null : from.wire(),
                        to.wire(),
                        actorUserId),
                Map.of(
                        "project_id", project.getId(),
                        "from_phase", from == null ? "" : from.wire(),
                        "to_phase", to.wire(),
                        "actor_user_id", actorUserId));

        if (to == ProjectPhase.MAINTENANCE) {
            outbox.append(
                    "projects",
                    new ProjectEvents.MaintenanceStarted(
                            UUID.randomUUID(),
                            now,
                            project.getId(),
                            project.getMaintenanceEndsAt()),
                    Map.of(
                            "project_id", project.getId(),
                            "ends_at",
                            project.getMaintenanceEndsAt() == null
                                    ? ""
                                    : project.getMaintenanceEndsAt().toString()));
        }
        if (to == ProjectPhase.CLOSED) {
            outbox.append(
                    "projects",
                    new ProjectEvents.ProjectClosed(UUID.randomUUID(), now, project.getId()),
                    Map.of("project_id", project.getId()));
        }
        log.info("Project {} transitioned {} → {}", project.getId(), from, to);
    }

    private void enforceGuard(Project project, ProjectPhase from, ProjectPhase to) {
        UUID projectId = project.getId();
        switch (from) {
            case CONTRACT_PENDING -> {
                if (contracts.countByProjectIdAndKind(projectId, "signed") == 0) {
                    throw new ApiException(
                            ApiErrorCode.VALIDATION_FAILED,
                            "Cannot advance without a signed contract.");
                }
            }
            case DISCUSSION -> {
                if (contracts.countByProjectIdAndKind(projectId, "unsigned") == 0) {
                    throw new ApiException(
                            ApiErrorCode.VALIDATION_FAILED,
                            "Upload the unsigned contract before advancing.");
                }
            }
            default -> { /* no exit criteria for this phase */ }
        }
    }

    public record NewProject(
            UUID clientUserId,
            String title,
            String description,
            BillingModel billingModel,
            Integer maintenanceDurationDays,
            Long totalAmountMinor,
            String currency,
            List<NewMilestone> milestones) {

        public NewProject {
            if (milestones == null) {
                milestones = new ArrayList<>();
            }
        }
    }

    public record NewMilestone(
            String title, String description, Long amountMinor, Instant dueAt) {}
}
