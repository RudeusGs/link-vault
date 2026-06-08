package com.linkvault.resources;

import com.linkvault.common.exception.BadRequestException;
import com.linkvault.folders.Folder;
import com.linkvault.folders.FolderService;
import com.linkvault.tags.TagService;
import com.linkvault.users.User;
import com.linkvault.users.UserContextService;
import com.linkvault.vaults.Vault;
import com.linkvault.vaults.VaultService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResourceSearchService {

    private final EntityManager entityManager;
    private final VaultService vaultService;
    private final FolderService folderService;
    private final TagService tagService;
    private final UserContextService userContextService;

    public ResourceSearchService(
        EntityManager entityManager,
        VaultService vaultService,
        FolderService folderService,
        TagService tagService,
        UserContextService userContextService
    ) {
        this.entityManager = entityManager;
        this.vaultService = vaultService;
        this.folderService = folderService;
        this.tagService = tagService;
        this.userContextService = userContextService;
    }

    @Transactional(readOnly = true)
    public List<Resource> search(
        String keyword,
        ResourceType type,
        UUID tagId,
        UUID vaultId,
        UUID folderId,
        Boolean rootOnly,
        Boolean favorite
    ) {
        User user = userContextService.getCurrentUser();
        Vault vault = vaultId == null ? null : vaultService.getVault(vaultId);
        Folder folder = folderId == null ? null : folderService.getFolder(folderId);
        if (tagId != null) {
            tagService.getTag(tagId);
        }
        if (vault != null && folder != null && !folder.getVault().getId().equals(vault.getId())) {
            throw new BadRequestException("Folder must belong to the selected vault");
        }

        return searchResources(
            user.getId(),
            cleanKeyword(keyword),
            type,
            tagId,
            vaultId,
            folderId,
            rootOnly,
            favorite
        );
    }

    private List<Resource> searchResources(
        UUID userId,
        String keyword,
        ResourceType type,
        UUID tagId,
        UUID vaultId,
        UUID folderId,
        Boolean rootOnly,
        Boolean favorite
    ) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Resource> query = cb.createQuery(Resource.class);
        Root<Resource> resource = query.from(Resource.class);

        resource.fetch("vault", JoinType.INNER);
        resource.fetch("folder", JoinType.LEFT);

        Join<Resource, Vault> vault = resource.join("vault", JoinType.INNER);
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(vault.get("user").<UUID>get("id"), userId));

        if (keyword != null) {
            String pattern = "%" + keyword.toLowerCase(Locale.ROOT) + "%";
            predicates.add(cb.or(
                cb.like(cb.lower(resource.<String>get("title")), pattern),
                cb.like(cb.lower(cb.coalesce(resource.<String>get("description"), "")), pattern),
                cb.like(cb.lower(cb.coalesce(resource.<String>get("url"), "")), pattern),
                cb.like(cb.lower(cb.coalesce(resource.<String>get("content"), "")), pattern)
            ));
        }

        if (type != null) {
            predicates.add(cb.equal(resource.get("resourceType"), type));
        }

        if (vaultId != null) {
            predicates.add(cb.equal(vault.<UUID>get("id"), vaultId));
        }

        if (folderId != null) {
            predicates.add(cb.equal(resource.get("folder").<UUID>get("id"), folderId));
        }

        if (Boolean.TRUE.equals(rootOnly)) {
            predicates.add(cb.isNull(resource.get("folder")));
        }

        if (favorite != null) {
            predicates.add(cb.equal(resource.<Boolean>get("isFavorite"), favorite));
        }

        if (tagId != null) {
            Subquery<UUID> tagSubquery = query.subquery(UUID.class);
            Root<ResourceTag> resourceTag = tagSubquery.from(ResourceTag.class);
            tagSubquery.select(resourceTag.<UUID>get("id"));
            tagSubquery.where(
                cb.equal(resourceTag.get("resource"), resource),
                cb.equal(resourceTag.get("tag").<UUID>get("id"), tagId)
            );
            predicates.add(cb.exists(tagSubquery));
        }

        query.select(resource)
            .distinct(true)
            .where(predicates.toArray(Predicate[]::new))
            .orderBy(cb.desc(resource.get("createdAt")));

        return entityManager.createQuery(query).getResultList();
    }

    private String cleanKeyword(String keyword) {
        return keyword == null || keyword.isBlank() ? null : keyword.trim();
    }
}
