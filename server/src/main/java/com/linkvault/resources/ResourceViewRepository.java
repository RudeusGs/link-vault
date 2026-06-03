package com.linkvault.resources;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface ResourceViewRepository extends JpaRepository<ResourceView, UUID> {

    @Transactional
    void deleteByResource_Id(UUID resourceId);
}
