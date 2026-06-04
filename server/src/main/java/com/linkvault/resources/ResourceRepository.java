package com.linkvault.resources;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResourceRepository extends JpaRepository<Resource, UUID> {

    @EntityGraph(attributePaths = {"vault", "folder"})
    List<Resource> findByVault_IdOrderByCreatedAtDesc(UUID vaultId);

    @EntityGraph(attributePaths = {"vault", "folder"})
    List<Resource> findByFolder_IdOrderByCreatedAtDesc(UUID folderId);

    @EntityGraph(attributePaths = {"vault", "folder"})
    List<Resource> findTop6ByVault_User_IdOrderByCreatedAtDesc(UUID userId);

    long countByVault_User_Id(UUID userId);

    long countByVault_User_IdAndResourceType(UUID userId, ResourceType resourceType);

    long countByVault_User_IdAndIsFavoriteTrue(UUID userId);
}
