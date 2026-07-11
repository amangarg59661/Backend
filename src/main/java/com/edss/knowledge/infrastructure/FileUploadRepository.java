package com.edss.knowledge.infrastructure;

import com.edss.knowledge.domain.FileUpload;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileUploadRepository extends JpaRepository<FileUpload, UUID> {

    Optional<FileUpload> findByUploadId(String uploadId);
}
