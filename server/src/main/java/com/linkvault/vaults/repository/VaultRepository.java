package com.linkvault.vaults.repository;

import com.linkvault.vaults.entity.Vault;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VaultRepository extends JpaRepository<Vault, UUID> {

    List<Vault> findByWorkspace_IdOrderByCreatedAtDesc(UUID workspaceId);

    java.util.Optional<Vault> findByIdAndWorkspace_Id(UUID id, UUID workspaceId);

    boolean existsByWorkspace_IdAndNameIgnoreCase(UUID workspaceId, String name);

    boolean existsByWorkspace_IdAndNameIgnoreCaseAndIdNot(UUID workspaceId, String name, UUID id);

    long countByWorkspace_Id(UUID workspaceId);
}