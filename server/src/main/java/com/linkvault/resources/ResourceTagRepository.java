package com.linkvault.resources;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface ResourceTagRepository extends JpaRepository<ResourceTag, UUID> {

    List<ResourceTag> findByResource_Id(UUID resourceId);

    boolean existsByResource_IdAndTag_Id(UUID resourceId, UUID tagId);

    long countByTag_Id(UUID tagId);

    @Transactional
    void deleteByResource_Id(UUID resourceId);

    @Transactional
    void deleteByTag_Id(UUID tagId);

    @Transactional
    void deleteByResource_IdAndTag_Id(UUID resourceId, UUID tagId);
}
