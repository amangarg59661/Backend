package com.edss.projects.api;

import com.edss.projects.api.dto.AdvancePhaseRequest;
import com.edss.projects.api.dto.ContractDto;
import com.edss.projects.api.dto.CreateProjectRequest;
import com.edss.projects.api.dto.MilestoneDto;
import com.edss.projects.api.dto.MilestoneReviewDto;
import com.edss.projects.api.dto.MilestoneReviewRequest;
import com.edss.projects.api.dto.OnboardingCallDto;
import com.edss.projects.api.dto.OnboardingCallUpsertRequest;
import com.edss.projects.api.dto.PhaseHistoryDto;
import com.edss.projects.api.dto.ProjectDto;
import com.edss.projects.api.dto.RegisterContractRequest;
import com.edss.projects.api.dto.ResetPhaseRequest;
import com.edss.projects.application.ContractService;
import com.edss.projects.application.MilestoneService;
import com.edss.projects.application.OnboardingCallService;
import com.edss.projects.application.ProjectService;
import com.edss.projects.domain.BillingModel;
import com.edss.projects.domain.Contract;
import com.edss.projects.domain.Milestone;
import com.edss.projects.domain.OnboardingCall;
import com.edss.projects.domain.Project;
import com.edss.projects.domain.ProjectPhase;
import com.edss.projects.domain.ReviewVerdict;
import com.edss.shared.api.ApiErrorCode;
import com.edss.shared.api.ApiException;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/projects")
@Tag(name = "projects", description = "Project lifecycle, milestones, contracts and onboarding calls.")
public class ProjectController {

    private final ProjectService projects;
    private final MilestoneService milestones;
    private final ContractService contracts;
    private final OnboardingCallService onboardingCalls;

    public ProjectController(
            ProjectService projects,
            MilestoneService milestones,
            ContractService contracts,
            OnboardingCallService onboardingCalls) {
        this.projects = projects;
        this.milestones = milestones;
        this.contracts = contracts;
        this.onboardingCalls = onboardingCalls;
    }

    // -----------------------------------------------------------------------
    // Listing + create
    // -----------------------------------------------------------------------

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public PaginatedResponse<ProjectDto> list(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "50") int limit) {
        boolean isStaff = isStaff(principal);
        List<ProjectDto> items =
                projects.list(principal.userId(), isStaff, limit).stream()
                        .map(ProjectController::toDto)
                        .toList();
        return new PaginatedResponse<>(items, null, false);
    }

    @GetMapping("/{projectId}")
    @PreAuthorize("isAuthenticated()")
    public ProjectDto fetch(
            @AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID projectId) {
        Project project = projects.fetch(projectId);
        enforceReadAccess(principal, project);
        return toDto(project);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('projects:project:create') or hasAuthority('projects:*') or hasAuthority('admin:*')")
    public ProjectDto create(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody CreateProjectRequest req) {
        List<ProjectService.NewMilestone> ms =
                req.milestones() == null
                        ? List.of()
                        : req.milestones().stream()
                                .map(
                                        m ->
                                                new ProjectService.NewMilestone(
                                                        m.title(),
                                                        m.description(),
                                                        m.amountMinor(),
                                                        m.dueAt()))
                                .toList();
        ProjectService.NewProject spec =
                new ProjectService.NewProject(
                        req.clientUserId(),
                        req.title(),
                        req.description(),
                        BillingModel.ofWire(req.billingModel()),
                        req.maintenanceDurationDays(),
                        req.totalAmountMinor(),
                        req.currency(),
                        ms);
        return toDto(projects.create(spec, principal.userId()));
    }

    // -----------------------------------------------------------------------
    // Phase machine
    // -----------------------------------------------------------------------

    @PostMapping("/{projectId}/phase/advance")
    @PreAuthorize("hasAuthority('projects:project:advance') or hasAuthority('projects:*') or hasAuthority('admin:*')")
    public ProjectDto advance(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID projectId,
            @Valid @RequestBody(required = false) AdvancePhaseRequest req) {
        String note = req == null ? null : req.note();
        return toDto(projects.advance(projectId, principal.userId(), note));
    }

    @PostMapping("/{projectId}/phase/reset")
    @PreAuthorize("hasAuthority('admin:project:override') or hasAuthority('admin:*')")
    public ProjectDto reset(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID projectId,
            @Valid @RequestBody ResetPhaseRequest req) {
        return toDto(
                projects.reset(
                        projectId,
                        principal.userId(),
                        ProjectPhase.ofWire(req.targetPhase()),
                        req.note()));
    }

    @GetMapping("/{projectId}/phase/history")
    @PreAuthorize("isAuthenticated()")
    public List<PhaseHistoryDto> phaseHistory(
            @AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID projectId) {
        enforceReadAccess(principal, projects.fetch(projectId));
        return projects.phaseHistory(projectId).stream()
                .map(
                        h ->
                                new PhaseHistoryDto(
                                        h.getId(),
                                        h.getProjectId(),
                                        h.getFromPhase(),
                                        h.getToPhase(),
                                        h.getActorUserId(),
                                        h.getNote(),
                                        h.getTransitionedAt()))
                .toList();
    }

    // -----------------------------------------------------------------------
    // Milestones
    // -----------------------------------------------------------------------

    @GetMapping("/{projectId}/milestones")
    @PreAuthorize("isAuthenticated()")
    public List<MilestoneDto> listMilestones(
            @AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID projectId) {
        enforceReadAccess(principal, projects.fetch(projectId));
        return milestones.listForProject(projectId).stream()
                .map(ProjectController::toDto)
                .toList();
    }

    @PostMapping("/{projectId}/milestones/{milestoneId}/submit")
    @PreAuthorize("hasAuthority('projects:milestone:submit') or hasAuthority('projects:*') or hasAuthority('admin:*')")
    public MilestoneDto submitMilestone(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID projectId,
            @PathVariable UUID milestoneId) {
        return toDto(milestones.submit(projectId, milestoneId, principal.userId()));
    }

    @PostMapping("/{projectId}/milestones/{milestoneId}/review")
    @PreAuthorize("isAuthenticated()")
    public MilestoneReviewDto reviewMilestone(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID projectId,
            @PathVariable UUID milestoneId,
            @Valid @RequestBody MilestoneReviewRequest req) {
        Project project = projects.fetch(projectId);
        enforceClientOrStaff(principal, project);
        return toDto(
                milestones.review(
                        projectId,
                        milestoneId,
                        ReviewVerdict.ofWire(req.verdict()),
                        req.comment(),
                        principal.userId()));
    }

    @GetMapping("/{projectId}/milestones/{milestoneId}/reviews")
    @PreAuthorize("isAuthenticated()")
    public List<MilestoneReviewDto> milestoneReviews(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID projectId,
            @PathVariable UUID milestoneId) {
        enforceReadAccess(principal, projects.fetch(projectId));
        return milestones.reviewsFor(milestoneId).stream()
                .map(ProjectController::toDto)
                .toList();
    }

    // -----------------------------------------------------------------------
    // Contracts
    // -----------------------------------------------------------------------

    @GetMapping("/{projectId}/contracts")
    @PreAuthorize("isAuthenticated()")
    public List<ContractDto> listContracts(
            @AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID projectId) {
        enforceReadAccess(principal, projects.fetch(projectId));
        return contracts.list(projectId).stream().map(ProjectController::toDto).toList();
    }

    @PostMapping("/{projectId}/contracts")
    @PreAuthorize("isAuthenticated()")
    public ContractDto registerContract(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID projectId,
            @Valid @RequestBody RegisterContractRequest req) {
        Project project = projects.fetch(projectId);
        Contract.Kind kind = Contract.Kind.ofWire(req.kind());
        // Only staff can upload unsigned; only the owning client (or staff) can
        // upload signed.
        if (kind == Contract.Kind.UNSIGNED && !isStaff(principal)) {
            throw new ApiException(ApiErrorCode.FORBIDDEN, "Staff only.");
        }
        if (kind == Contract.Kind.SIGNED) {
            enforceClientOrStaff(principal, project);
        }
        return toDto(
                contracts.register(
                        projectId, kind, req.storageKey(), req.sha256(), principal.userId()));
    }

    // -----------------------------------------------------------------------
    // Onboarding call
    // -----------------------------------------------------------------------

    @GetMapping("/{projectId}/onboarding-call")
    @PreAuthorize("isAuthenticated()")
    public OnboardingCallDto onboardingCall(
            @AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID projectId) {
        enforceReadAccess(principal, projects.fetch(projectId));
        return onboardingCalls
                .forProject(projectId)
                .map(ProjectController::toDto)
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND, "No call scheduled."));
    }

    @PutMapping("/{projectId}/onboarding-call")
    @PreAuthorize("hasAuthority('projects:project:advance') or hasAuthority('projects:*') or hasAuthority('admin:*')")
    public OnboardingCallDto upsertOnboardingCall(
            @PathVariable UUID projectId, @Valid @RequestBody OnboardingCallUpsertRequest req) {
        return toDto(
                onboardingCalls.createOrUpdate(
                        projectId,
                        req.provider(),
                        req.scheduledAt(),
                        req.meetingUrl(),
                        req.externalRef()));
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static boolean isStaff(AuthenticatedUser principal) {
        return "staff".equals(principal.primaryRole()) || principal.hasBothRoles();
    }

    private static void enforceReadAccess(AuthenticatedUser principal, Project project) {
        if (isStaff(principal)) {
            return;
        }
        if (!project.getOwnerUserId().equals(principal.userId())) {
            throw new ApiException(ApiErrorCode.FORBIDDEN, "Not your project.");
        }
    }

    private static void enforceClientOrStaff(AuthenticatedUser principal, Project project) {
        if (isStaff(principal)) {
            return;
        }
        if (!project.getOwnerUserId().equals(principal.userId())) {
            throw new ApiException(ApiErrorCode.FORBIDDEN, "Not your project.");
        }
    }

    private static ProjectDto toDto(Project p) {
        return new ProjectDto(
                p.getId(),
                p.getOwnerUserId(),
                p.getTitle(),
                p.getDescription(),
                p.getStatus(),
                p.getPhase().wire(),
                p.getBillingModel().wire(),
                p.getMaintenanceDurationDays(),
                p.getMaintenanceStartsAt(),
                p.getMaintenanceEndsAt(),
                p.getTotalAmountMinor(),
                p.getCurrency(),
                p.getCreatedAt(),
                p.getUpdatedAt());
    }

    private static MilestoneDto toDto(Milestone m) {
        return new MilestoneDto(
                m.getId(),
                m.getProjectId(),
                m.getOrdinal(),
                m.getTitle(),
                m.getDescription(),
                m.getAmountMinor(),
                m.getStatus().wire(),
                m.getDueAt(),
                m.getSubmittedAt(),
                m.getApprovedAt());
    }

    private static MilestoneReviewDto toDto(com.edss.projects.domain.MilestoneReview r) {
        return new MilestoneReviewDto(
                r.getId(),
                r.getMilestoneId(),
                r.getVerdict().wire(),
                r.getComment(),
                r.getReviewedByUserId(),
                r.getReviewedAt());
    }

    private static ContractDto toDto(Contract c) {
        return new ContractDto(
                c.getId(),
                c.getProjectId(),
                c.getKind().wire(),
                c.getStorageKey(),
                c.getSha256(),
                c.getUploadedByUserId(),
                c.getUploadedAt());
    }

    private static OnboardingCallDto toDto(OnboardingCall call) {
        return new OnboardingCallDto(
                call.getId(),
                call.getProjectId(),
                call.getProvider(),
                call.getScheduledAt(),
                call.getMeetingUrl(),
                call.getExternalRef(),
                call.getStatus().wire());
    }
}
