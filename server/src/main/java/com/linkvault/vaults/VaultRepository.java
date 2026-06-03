package com.linkvault.vaults;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VaultRepository extends JpaRepository<Vault, UUID> {

    List<Vault> findByUser_IdOrderByCreatedAtDesc(UUID userId);

    long countByUser_Id(UUID userId);
}
