package com.edss.identity.infrastructure;

import com.edss.identity.domain.BackupCode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface BackupCodeRepository extends JpaRepository<BackupCode, UUID> {

    // Retained for callers that still key on the SHA-256 digest;
    // BackupCodeService switched to BCrypt (S-12) and uses findByUserId.
    Optional<BackupCode> findByUserIdAndCodeHash(UUID userId, String codeHash);

    List<BackupCode> findByUserId(UUID userId);

    List<BackupCode> findByUserIdAndUsedAtIsNull(UUID userId);

    long countByUserIdAndUsedAtIsNull(UUID userId);

    @Modifying
    @Query("DELETE FROM BackupCode b WHERE b.userId = :userId")
    void deleteAllForUser(UUID userId);
}
