package com.edss.knowledge.domain.events;

import com.edss.shared.events.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public final class KnowledgeEvents {

    private KnowledgeEvents() {}

    public record FileUploaded(
            UUID eventId,
            Instant occurredAt,
            UUID fileId,
            UUID ownerUserId,
            String kind,
            UUID projectId,
            UUID milestoneId,
            long sizeBytes)
            implements DomainEvent {
        @Override
        public String eventType() {
            return "knowledge.file_uploaded";
        }

        @Override
        public int eventVersion() {
            return 1;
        }

        @Override
        public String aggregateType() {
            return "file";
        }

        @Override
        public UUID aggregateId() {
            return fileId;
        }
    }
}
