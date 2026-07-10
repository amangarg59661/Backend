package com.edss.identity.infrastructure;

import com.edss.identity.domain.PasswordHistoryEntry;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface PasswordHistoryRepository extends JpaRepository<PasswordHistoryEntry, UUID> {

    List<PasswordHistoryEntry> findByUserIdOrderByCreatedAtDesc(UUID userId, Limit limit);

    @Modifying
    @Query(
            value =
                    "DELETE FROM identity.password_history WHERE user_id = :userId AND id NOT IN"
                        + " (SELECT id FROM identity.password_history WHERE user_id = :userId"
                        + " ORDER BY created_at DESC LIMIT :keep)",
            nativeQuery = true)
    void trimToLatest(UUID userId, int keep);
}
