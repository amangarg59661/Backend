package com.edss.projects.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.edss.projects.domain.ProjectPhase;
import org.junit.jupiter.api.Test;

class ProjectPhaseTransitionTest {

    @Test
    void everyNonTerminalPhaseAdvances() {
        for (ProjectPhase phase : ProjectPhase.values()) {
            if (phase == ProjectPhase.CLOSED) {
                continue;
            }
            ProjectPhase next = ProjectPhaseTransition.next(phase);
            assertThat(next).isNotNull();
            assertThat(next).isNotEqualTo(phase);
        }
    }

    @Test
    void discussionAdvancesToContractPending() {
        assertThat(ProjectPhaseTransition.next(ProjectPhase.DISCUSSION))
                .isEqualTo(ProjectPhase.CONTRACT_PENDING);
    }

    @Test
    void maintenanceAdvancesToClosed() {
        assertThat(ProjectPhaseTransition.next(ProjectPhase.MAINTENANCE))
                .isEqualTo(ProjectPhase.CLOSED);
    }

    @Test
    void closedRejectsFurtherAdvance() {
        assertThatThrownBy(() -> ProjectPhaseTransition.next(ProjectPhase.CLOSED))
                .isInstanceOf(IllegalStateException.class);
    }
}
