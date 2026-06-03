package com.linkvault.folders;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FolderRepository extends JpaRepository<Folder, UUID> {

    List<Folder> findByVault_IdOrderBySortOrderAscNameAsc(UUID vaultId);

    List<Folder> findByVault_IdAndParentIsNullOrderBySortOrderAscNameAsc(UUID vaultId);

    List<Folder> findByParent_IdOrderBySortOrderAscNameAsc(UUID parentId);

    boolean existsByVault_IdAndParentIsNullAndNameIgnoreCase(UUID vaultId, String name);

    boolean existsByVault_IdAndParentIsNullAndNameIgnoreCaseAndIdNot(UUID vaultId, String name, UUID id);

    boolean existsByVault_IdAndParent_IdAndNameIgnoreCase(UUID vaultId, UUID parentId, String name);

    boolean existsByVault_IdAndParent_IdAndNameIgnoreCaseAndIdNot(UUID vaultId, UUID parentId, String name, UUID id);

    long countByVault_User_Id(UUID userId);
}
