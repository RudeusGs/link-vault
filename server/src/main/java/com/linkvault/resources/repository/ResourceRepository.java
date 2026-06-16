package com.linkvault.resources.repository;

import com.linkvault.resources.entity.Resource;
import com.linkvault.resources.enums.ResourceType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ResourceRepository extends JpaRepository<Resource, UUID> {

    @EntityGraph(attributePaths = {"vault", "folder"})
    Page<Resource> findByVault_Id(UUID vaultId, Pageable pageable);

    List<Resource> findByVault_IdOrderByCreatedAtDesc(UUID vaultId);

    @EntityGraph(attributePaths = {"vault", "folder"})
    Page<Resource> findByFolder_Id(UUID folderId, Pageable pageable);

    List<Resource> findByFolder_IdOrderByCreatedAtDesc(UUID folderId);

    @EntityGraph(attributePaths = {"vault", "folder"})
    Optional<Resource> findByIdAndVault_Workspace_Id(UUID id, UUID workspaceId);

    @EntityGraph(attributePaths = {"vault", "folder"})
    Page<Resource> findByVault_IdAndVault_Workspace_Id(UUID vaultId, UUID workspaceId, Pageable pageable);

    @EntityGraph(attributePaths = {"vault", "folder"})
    Page<Resource> findByFolder_IdAndVault_Workspace_Id(UUID folderId, UUID workspaceId, Pageable pageable);

    @EntityGraph(attributePaths = {"vault", "folder"})
    List<Resource> findTop6ByVault_Workspace_IdOrderByCreatedAtDesc(UUID workspaceId);

    long countByVault_Workspace_Id(UUID workspaceId);

    long countByVault_Workspace_IdAndResourceType(UUID workspaceId, ResourceType resourceType);

    long countByVault_Workspace_IdAndIsFavoriteTrue(UUID workspaceId);

    @Query("""
        select coalesce(sum(r.fileSize), 0)
        from Resource r
        where r.vault.workspace.id = :workspaceId
          and r.deletedAt is null
        """)
    long sumFileSizeByWorkspaceId(UUID workspaceId);
}