package com.edss.projects.infrastructure;

import com.edss.projects.domain.ProjectPhaseHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectPhaseHistoryRepository extends JpaRepository<ProjectPhaseHistory, UUID> {

    List<ProjectPhaseHistory> findByProjectIdOrderByTransitionedAtDesc(UUID projectId);
}
