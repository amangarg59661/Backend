package com.edss.projects.application;

import com.edss.projects.infrastructure.ProjectRepository;
import com.edss.projects.spi.ProjectsQuery;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class ProjectsQueryService implements ProjectsQuery {

    private final ProjectRepository projects;

    ProjectsQueryService(ProjectRepository projects) {
        this.projects = projects;
    }

    @Override
    public Optional<ProjectSummary> findProject(UUID projectId) {
        return projects.findById(projectId)
                .map(
                        p ->
                                new ProjectSummary(
                                        p.getId(),
                                        p.getOwnerUserId(),
                                        p.getPhase().wire(),
                                        p.getMaintenanceStartsAt(),
                                        p.getMaintenanceEndsAt()));
    }
}
