package com.linkvault.vaults;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VaultRepository extends JpaRepository<Vault, UUID> {

    List<Vault> findByUser_IdOrderByCreatedAtDesc(UUID userId);

    boolean existsByUser_IdAndNameIgnoreCase(UUID userId, String name);

    boolean existsByUser_IdAndNameIgnoreCaseAndIdNot(UUID userId, String name, UUID id);

    long countByUser_Id(UUID userId);
}
