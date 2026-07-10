package com.edss.projects.infrastructure;

import com.edss.projects.domain.Contract;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractRepository extends JpaRepository<Contract, UUID> {

    List<Contract> findByProjectIdOrderByUploadedAtDesc(UUID projectId);

    long countByProjectIdAndKind(UUID projectId, String kind);
}
