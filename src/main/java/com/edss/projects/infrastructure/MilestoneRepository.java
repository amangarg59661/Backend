package com.edss.projects.infrastructure;

import com.edss.projects.domain.Milestone;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MilestoneRepository extends JpaRepository<Milestone, UUID> {

    List<Milestone> findByProjectIdOrderByOrdinalAsc(UUID projectId);
}
