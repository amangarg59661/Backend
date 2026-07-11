package com.edss.projects.spi;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Read-only port other modules use to inspect project state without touching
 * {@code projects.*} tables. When projects extracts to its own service, this
 * becomes the remote client interface with no caller changes.
 */
public interface ProjectsQuery {

    Optional<ProjectSummary> findProject(UUID projectId);

    record ProjectSummary(
            UUID id,
            UUID ownerUserId,
            String phase,
            Instant maintenanceStartsAt,
            Instant maintenanceEndsAt) {

        public boolean isInMaintenanceWindow(Instant now) {
            return "maintenance".equals(phase)
                    && maintenanceStartsAt != null
                    && maintenanceEndsAt != null
                    && !now.isBefore(maintenanceStartsAt)
                    && now.isBefore(maintenanceEndsAt);
        }
    }
}
