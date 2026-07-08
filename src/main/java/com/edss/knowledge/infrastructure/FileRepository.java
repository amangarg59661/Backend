package com.edss.knowledge.infrastructure;

import com.edss.knowledge.domain.FileRecord;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileRepository extends JpaRepository<FileRecord, UUID> {}
