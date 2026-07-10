package com.edss.projects.application;

import com.edss.projects.domain.Project;
import com.edss.projects.domain.ProjectPhase;
import java.util.EnumMap;
import java.util.Map;

/**
 * Sequential state machine over {@link ProjectPhase}. The forward map is
 * fixed here; guards live at the call site (e.g. contract_pending →
 * contract_signed only after a signed contract row exists). Backward moves
 * bypass this map via the reset path (admin-only).
 */
public final class ProjectPhaseTransition {

    private static final Map<ProjectPhase, ProjectPhase> FORWARD = new EnumMap<>(ProjectPhase.class);

    static {
        FORWARD.put(ProjectPhase.DISCUSSION, ProjectPhase.CONTRACT_PENDING);
        FORWARD.put(ProjectPhase.CONTRACT_PENDING, ProjectPhase.CONTRACT_SIGNED);
        FORWARD.put(ProjectPhase.CONTRACT_SIGNED, ProjectPhase.ONBOARDING_SCHEDULED);
        FORWARD.put(ProjectPhase.ONBOARDING_SCHEDULED, ProjectPhase.ONBOARDING_COMPLETE);
        FORWARD.put(ProjectPhase.ONBOARDING_COMPLETE, ProjectPhase.ADVANCE_INVOICED);
        FORWARD.put(ProjectPhase.ADVANCE_INVOICED, ProjectPhase.ASSETS_PENDING);
        FORWARD.put(ProjectPhase.ASSETS_PENDING, ProjectPhase.ASSETS_RECEIVED);
        FORWARD.put(ProjectPhase.ASSETS_RECEIVED, ProjectPhase.IN_PROGRESS);
        FORWARD.put(ProjectPhase.IN_PROGRESS, ProjectPhase.CLIENT_REVIEW);
        FORWARD.put(ProjectPhase.CLIENT_REVIEW, ProjectPhase.FINAL_SUBMISSION);
        FORWARD.put(ProjectPhase.FINAL_SUBMISSION, ProjectPhase.FINAL_INVOICED);
        FORWARD.put(ProjectPhase.FINAL_INVOICED, ProjectPhase.MAINTENANCE);
        FORWARD.put(ProjectPhase.MAINTENANCE, ProjectPhase.CLOSED);
    }

    private ProjectPhaseTransition() {}

    public static ProjectPhase next(ProjectPhase current) {
        ProjectPhase target = FORWARD.get(current);
        if (target == null) {
            throw new IllegalStateException("No forward transition from " + current.wire());
        }
        return target;
    }

    public static boolean canAdvance(Project project) {
        return FORWARD.containsKey(project.getPhase());
    }
}
