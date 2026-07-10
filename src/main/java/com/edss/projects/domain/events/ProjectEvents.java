package com.edss.projects.domain.events;

import com.edss.shared.events.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public final class ProjectEvents {

    private ProjectEvents() {}

    private abstract static class BaseProjectEvent implements DomainEvent {
        @Override
        public String aggregateType() {
            return "project";
        }
    }

    public record ProjectCreated(
            UUID eventId,
            Instant occurredAt,
            UUID projectId,
            UUID ownerUserId,
            String billingModel)
            implements DomainEvent {
        @Override
        public String eventType() {
            return "projects.project_created";
        }

        @Override
        public int eventVersion() {
            return 1;
        }

        @Override
        public String aggregateType() {
            return "project";
        }

        @Override
        public UUID aggregateId() {
            return projectId;
        }
    }

    public record PhaseTransitioned(
            UUID eventId,
            Instant occurredAt,
            UUID projectId,
            String fromPhase,
            String toPhase,
            UUID actorUserId)
            implements DomainEvent {
        @Override
        public String eventType() {
            return "projects.phase_transitioned";
        }

        @Override
        public int eventVersion() {
            return 1;
        }

        @Override
        public String aggregateType() {
            return "project";
        }

        @Override
        public UUID aggregateId() {
            return projectId;
        }
    }

    public record ContractUploaded(
            UUID eventId,
            Instant occurredAt,
            UUID projectId,
            UUID contractId,
            String kind,
            UUID uploadedByUserId)
            implements DomainEvent {
        @Override
        public String eventType() {
            return "projects.contract_uploaded";
        }

        @Override
        public int eventVersion() {
            return 1;
        }

        @Override
        public String aggregateType() {
            return "project";
        }

        @Override
        public UUID aggregateId() {
            return projectId;
        }
    }

    public record MilestoneSubmitted(
            UUID eventId, Instant occurredAt, UUID projectId, UUID milestoneId, int ordinal)
            implements DomainEvent {
        @Override
        public String eventType() {
            return "projects.milestone_submitted";
        }

        @Override
        public int eventVersion() {
            return 1;
        }

        @Override
        public String aggregateType() {
            return "milestone";
        }

        @Override
        public UUID aggregateId() {
            return milestoneId;
        }
    }

    public record MilestoneReviewed(
            UUID eventId,
            Instant occurredAt,
            UUID projectId,
            UUID milestoneId,
            String verdict,
            UUID reviewerUserId)
            implements DomainEvent {
        @Override
        public String eventType() {
            return "projects.milestone_reviewed";
        }

        @Override
        public int eventVersion() {
            return 1;
        }

        @Override
        public String aggregateType() {
            return "milestone";
        }

        @Override
        public UUID aggregateId() {
            return milestoneId;
        }
    }

    public record OnboardingScheduled(
            UUID eventId,
            Instant occurredAt,
            UUID projectId,
            String provider,
            Instant scheduledAt)
            implements DomainEvent {
        @Override
        public String eventType() {
            return "projects.onboarding_scheduled";
        }

        @Override
        public int eventVersion() {
            return 1;
        }

        @Override
        public String aggregateType() {
            return "project";
        }

        @Override
        public UUID aggregateId() {
            return projectId;
        }
    }

    public record MaintenanceStarted(UUID eventId, Instant occurredAt, UUID projectId, Instant endsAt)
            implements DomainEvent {
        @Override
        public String eventType() {
            return "projects.maintenance_started";
        }

        @Override
        public int eventVersion() {
            return 1;
        }

        @Override
        public String aggregateType() {
            return "project";
        }

        @Override
        public UUID aggregateId() {
            return projectId;
        }
    }

    public record ProjectClosed(UUID eventId, Instant occurredAt, UUID projectId)
            implements DomainEvent {
        @Override
        public String eventType() {
            return "projects.project_closed";
        }

        @Override
        public int eventVersion() {
            return 1;
        }

        @Override
        public String aggregateType() {
            return "project";
        }

        @Override
        public UUID aggregateId() {
            return projectId;
        }
    }
}
