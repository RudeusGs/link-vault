package com.linkvault.workspaces.repository;

import com.linkvault.workspaces.entity.Workspace;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceRepository extends JpaRepository<Workspace, UUID> {

    boolean existsBySlugIgnoreCase(String slug);

    Optional<Workspace> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsBySlugIgnoreCaseAndIdNot(String slug, UUID id);
}