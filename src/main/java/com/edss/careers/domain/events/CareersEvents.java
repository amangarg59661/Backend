package com.edss.careers.domain.events;

import com.edss.shared.events.DomainEvent;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public final class CareersEvents {

    private CareersEvents() {}

    public record JobPostingPublished(
            UUID eventId, Instant occurredAt, UUID postingId, String slug, String title)
            implements DomainEvent {
        @Override
        public String eventType() {
            return "careers.posting_published";
        }

        @Override
        public int eventVersion() {
            return 1;
        }

        @Override
        public String aggregateType() {
            return "job_posting";
        }

        @Override
        public UUID aggregateId() {
            return postingId;
        }
    }

    public record ApplicationSubmitted(
            UUID eventId,
            Instant occurredAt,
            UUID applicationId,
            UUID postingId,
            String applicantEmail,
            String applicantName,
            String postingTitle)
            implements DomainEvent {
        @Override
        public String eventType() {
            return "careers.application_submitted";
        }

        @Override
        public int eventVersion() {
            return 1;
        }

        @Override
        public String aggregateType() {
            return "job_application";
        }

        @Override
        public UUID aggregateId() {
            return applicationId;
        }
    }

    public record ApplicationReviewed(
            UUID eventId,
            Instant occurredAt,
            UUID applicationId,
            UUID postingId,
            String applicantEmail,
            String applicantName,
            String status,
            String postingTitle)
            implements DomainEvent {
        @Override
        public String eventType() {
            return "careers.application_reviewed";
        }

        @Override
        public int eventVersion() {
            return 1;
        }

        @Override
        public String aggregateType() {
            return "job_application";
        }

        @Override
        public UUID aggregateId() {
            return applicationId;
        }
    }
}
