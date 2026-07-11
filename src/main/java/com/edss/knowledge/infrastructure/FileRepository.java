package com.edss.knowledge.infrastructure;

import com.edss.knowledge.domain.FileRecord;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileRepository extends JpaRepository<FileRecord, UUID> {

    List<FileRecord> findByOwnerUserIdOrderByCreatedAtDesc(UUID ownerUserId, Limit limit);

    List<FileRecord> findByProjectIdOrderByCreatedAtDesc(UUID projectId, Limit limit);

    List<FileRecord> findByMilestoneIdOrderByCreatedAtDesc(UUID milestoneId, Limit limit);

    List<FileRecord> findAllByOrderByCreatedAtDesc(Limit limit);
}
