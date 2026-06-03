package com.linkvault.folders;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FolderRepository extends JpaRepository<Folder, UUID> {

    List<Folder> findByVault_IdOrderBySortOrderAscNameAsc(UUID vaultId);

    List<Folder> findByVault_IdAndParentIsNullOrderBySortOrderAscNameAsc(UUID vaultId);

    List<Folder> findByParent_IdOrderBySortOrderAscNameAsc(UUID parentId);

    long countByVault_User_Id(UUID userId);
}
