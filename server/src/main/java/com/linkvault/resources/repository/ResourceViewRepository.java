package com.linkvault.resources.repository;

import com.linkvault.resources.entity.ResourceView;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface ResourceViewRepository extends JpaRepository<ResourceView, UUID> {

    @Transactional
    void deleteByResource_Id(UUID resourceId);
}