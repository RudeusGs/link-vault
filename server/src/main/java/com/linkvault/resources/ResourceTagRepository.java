package com.linkvault.resources;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface ResourceTagRepository extends JpaRepository<ResourceTag, UUID> {

    List<ResourceTag> findByResource_Id(UUID resourceId);

    @EntityGraph(attributePaths = {"resource", "tag"})
    List<ResourceTag> findByResource_IdIn(List<UUID> resourceIds);

    @Query("""
        select rt.tag.id, count(rt.id)
        from ResourceTag rt
        where rt.tag.id in :tagIds
        group by rt.tag.id
        """)
    List<Object[]> countUsageByTagIds(@Param("tagIds") List<UUID> tagIds);

    boolean existsByResource_IdAndTag_Id(UUID resourceId, UUID tagId);

    long countByTag_Id(UUID tagId);

    @Transactional
    void deleteByResource_Id(UUID resourceId);

    @Transactional
    void deleteByTag_Id(UUID tagId);

    @Transactional
    void deleteByResource_IdAndTag_Id(UUID resourceId, UUID tagId);
}
