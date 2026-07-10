package com.edss.projects.infrastructure;

import com.edss.projects.domain.Project;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    List<Project> findByOwnerUserIdOrderByCreatedAtDesc(UUID ownerUserId, Limit limit);

    List<Project> findAllByOrderByCreatedAtDesc(Limit limit);
}
