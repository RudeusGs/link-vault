package com.linkvault.resources;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ResourceRepository extends JpaRepository<Resource, UUID> {

    List<Resource> findByVault_IdOrderByCreatedAtDesc(UUID vaultId);

    List<Resource> findByFolder_IdOrderByCreatedAtDesc(UUID folderId);

    List<Resource> findTop6ByVault_User_IdOrderByCreatedAtDesc(UUID userId);

    long countByVault_User_Id(UUID userId);

    long countByVault_User_IdAndResourceType(UUID userId, ResourceType resourceType);

    long countByVault_User_IdAndIsFavoriteTrue(UUID userId);

    @Query("""
        select distinct r from Resource r
        where r.vault.user.id = :userId
        and (:keyword is null
            or lower(r.title) like lower(concat('%', :keyword, '%'))
            or lower(coalesce(r.description, '')) like lower(concat('%', :keyword, '%'))
            or lower(coalesce(r.url, '')) like lower(concat('%', :keyword, '%'))
            or lower(coalesce(r.content, '')) like lower(concat('%', :keyword, '%')))
        and (:type is null or r.resourceType = :type)
        and (:vaultId is null or r.vault.id = :vaultId)
        and (:folderId is null or r.folder.id = :folderId)
        and (:favorite is null or r.isFavorite = :favorite)
        and (:tagId is null or exists (
            select rt.id from ResourceTag rt
            where rt.resource = r and rt.tag.id = :tagId
        ))
        order by r.createdAt desc
        """)
    List<Resource> search(
        @Param("userId") UUID userId,
        @Param("keyword") String keyword,
        @Param("type") ResourceType type,
        @Param("tagId") UUID tagId,
        @Param("vaultId") UUID vaultId,
        @Param("folderId") UUID folderId,
        @Param("favorite") Boolean favorite
    );
}
