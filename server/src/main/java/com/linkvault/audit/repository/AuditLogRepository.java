package com.linkvault.audit.repository;

import com.linkvault.audit.entity.AuditLog;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    @EntityGraph(attributePaths = {"actor", "workspace"})
    List<AuditLog> findTop100ByWorkspace_IdOrderByCreatedAtDesc(UUID workspaceId);
}