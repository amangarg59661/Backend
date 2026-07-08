package com.edss.identity.infrastructure;

import com.edss.identity.domain.Permission;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PermissionRepository
        extends JpaRepository<Permission, Permission.PermissionId> {

    @Query("SELECT p.id.permission FROM Permission p WHERE p.id.userId = :userId")
    List<String> findPermissionStringsByUserId(UUID userId);
}
