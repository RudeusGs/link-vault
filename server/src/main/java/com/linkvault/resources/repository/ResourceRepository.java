package com.linkvault.resources.repository;

import com.linkvault.folders.entity.Folder;
import com.linkvault.resources.entity.Resource;
import com.linkvault.resources.enums.ResourceType;
import com.linkvault.vaults.entity.Vault;
import com.linkvault.workspaces.entity.Workspace;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ResourceRepository extends JpaRepository<Resource, UUID> {

    @EntityGraph(attributePaths = {"vault", "folder"})
    List<Resource> findByVault_IdOrderByCreatedAtDesc(UUID vaultId);

    @EntityGraph(attributePaths = {"vault", "folder"})
    List<Resource> findByFolder_IdOrderByCreatedAtDesc(UUID folderId);

    @EntityGraph(attributePaths = {"vault", "folder"})
    Optional<Resource> findByIdAndVault_Workspace_Id(UUID id, UUID workspaceId);

    @EntityGraph(attributePaths = {"vault", "folder"})
    List<Resource> findByVault_IdAndVault_Workspace_IdOrderByCreatedAtDesc(UUID vaultId, UUID workspaceId);

    @EntityGraph(attributePaths = {"vault", "folder"})
    List<Resource> findByFolder_IdAndVault_Workspace_IdOrderByCreatedAtDesc(UUID folderId, UUID workspaceId);

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