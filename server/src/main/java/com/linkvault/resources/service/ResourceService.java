package com.linkvault.resources.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.linkvault.audit.service.AuditLogService;
import com.linkvault.common.exception.BadRequestException;
import com.linkvault.common.exception.ErrorCode;
import com.linkvault.common.exception.NotFoundException;
import com.linkvault.common.redis.RedisCacheInvalidationService;
import com.linkvault.common.redis.RedisCacheService;
import com.linkvault.common.redis.RedisKeys;
import com.linkvault.common.redis.RedisProperties;
import com.linkvault.folders.entity.Folder;
import com.linkvault.folders.service.FolderService;
import com.linkvault.resources.dto.DocumentPreviewResponse;
import com.linkvault.resources.dto.LinkPreviewResponse;
import com.linkvault.resources.dto.ResourceFileContent;
import com.linkvault.resources.dto.ResourcePreviewResponse;
import com.linkvault.resources.dto.ResourceRequest;
import com.linkvault.common.pagination.PageResponse;
import com.linkvault.resources.dto.ResourceResponse;
import com.linkvault.resources.entity.Resource;
import com.linkvault.resources.entity.ResourceTag;
import com.linkvault.resources.entity.ResourceView;
import com.linkvault.resources.enums.ResourceType;
import com.linkvault.resources.mapper.ResourceMapper;
import com.linkvault.resources.repository.ResourceRepository;
import com.linkvault.resources.repository.ResourceTagRepository;
import com.linkvault.resources.util.FileValidationUtil;
import com.linkvault.resources.repository.ResourceViewRepository;
import com.linkvault.storage.dto.StorageResult;
import com.linkvault.storage.service.StorageService;
import com.linkvault.tags.entity.Tag;
import com.linkvault.tags.service.TagService;
import com.linkvault.users.entity.User;
import com.linkvault.users.service.UserContextService;
import com.linkvault.vaults.entity.Vault;
import com.linkvault.vaults.service.VaultService;
import com.linkvault.workspaces.service.QuotaService;
import com.linkvault.workspaces.service.WorkspaceService;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.linkvault.common.outbox.OutboxService;
import com.linkvault.common.rabbitmq.event.LinkPreviewRequestedEvent;

@Service
public class ResourceService {

    private static final TypeReference<PageResponse<ResourceResponse>> RESOURCE_PAGE_TYPE = new TypeReference<>() {
    };

    private final ResourceRepository resourceRepository;
    private final ResourceTagRepository resourceTagRepository;
    private final ResourceViewRepository resourceViewRepository;
    private final VaultService vaultService;
    private final FolderService folderService;
    private final TagService tagService;
    private final StorageService storageService;
    private final LinkPreviewService linkPreviewService;
    private final UserContextService userContextService;
    private final ResourceMapper resourceMapper;
    private final ResourceSearchService resourceSearchService;
    private final ResourcePreviewService resourcePreviewService;
    private final ResourceCleanupService resourceCleanupService;
    private final WorkspaceService workspaceService;
    private final QuotaService quotaService;
    private final AuditLogService auditLogService;
    private final RedisCacheService redisCacheService;
    private final RedisProperties redisProperties;
    private final RedisCacheInvalidationService cacheInvalidationService;
    private final OutboxService outboxService;

    public ResourceService(
        ResourceRepository resourceRepository,
        ResourceTagRepository resourceTagRepository,
        ResourceViewRepository resourceViewRepository,
        VaultService vaultService,
        FolderService folderService,
        TagService tagService,
        StorageService storageService,
        LinkPreviewService linkPreviewService,
        UserContextService userContextService,
        ResourceMapper resourceMapper,
        ResourceSearchService resourceSearchService,
        ResourcePreviewService resourcePreviewService,
        ResourceCleanupService resourceCleanupService,
        WorkspaceService workspaceService,
        QuotaService quotaService,
        AuditLogService auditLogService,
        RedisCacheService redisCacheService,
        RedisProperties redisProperties,
        RedisCacheInvalidationService cacheInvalidationService,
        OutboxService outboxService
    ) {
        this.resourceRepository = resourceRepository;
        this.resourceTagRepository = resourceTagRepository;
        this.resourceViewRepository = resourceViewRepository;
        this.vaultService = vaultService;
        this.folderService = folderService;
        this.tagService = tagService;
        this.storageService = storageService;
        this.linkPreviewService = linkPreviewService;
        this.userContextService = userContextService;
        this.resourceMapper = resourceMapper;
        this.resourceSearchService = resourceSearchService;
        this.resourcePreviewService = resourcePreviewService;
        this.resourceCleanupService = resourceCleanupService;
        this.workspaceService = workspaceService;
        this.quotaService = quotaService;
        this.auditLogService = auditLogService;
        this.redisCacheService = redisCacheService;
        this.redisProperties = redisProperties;
        this.cacheInvalidationService = cacheInvalidationService;
        this.outboxService = outboxService;
    }

    @Transactional(readOnly = true)
    public PageResponse<ResourceResponse> listAll(org.springframework.data.domain.Pageable pageable) {
        return search(null, null, null, null, null, null, null, pageable);
    }

    @Transactional(readOnly = true)
    public PageResponse<ResourceResponse> listByVault(UUID vaultId, org.springframework.data.domain.Pageable pageable) {
        Vault vault = vaultService.getVault(vaultId);
        return cachedResourcePage(
            resourceWorkspaceId(vault),
            RedisKeys.pageSignature("vault", vaultId, pageableSignature(pageable)),
            () -> resourceMapper.toPageResponse(resourceRepository.findByVault_Id(vaultId, pageable))
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<ResourceResponse> listByVault(UUID workspaceId, UUID vaultId, org.springframework.data.domain.Pageable pageable) {
        vaultService.getVault(workspaceId, vaultId);
        return cachedResourcePage(
            workspaceId,
            RedisKeys.pageSignature("workspace-vault", vaultId, pageableSignature(pageable)),
            () -> resourceMapper.toPageResponse(
                resourceRepository.findByVault_IdAndVault_Workspace_Id(vaultId, workspaceId, pageable)
            )
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<ResourceResponse> listByFolder(UUID folderId, org.springframework.data.domain.Pageable pageable) {
        Folder folder = folderService.getFolder(folderId);
        return cachedResourcePage(
            resourceWorkspaceId(folder),
            RedisKeys.pageSignature("folder", folder.getId(), pageableSignature(pageable)),
            () -> resourceMapper.toPageResponse(resourceRepository.findByFolder_Id(folder.getId(), pageable))
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<ResourceResponse> listByFolder(UUID workspaceId, UUID folderId, org.springframework.data.domain.Pageable pageable) {
        Folder folder = folderService.getFolder(workspaceId, folderId);
        return cachedResourcePage(
            workspaceId,
            RedisKeys.pageSignature("workspace-folder", folder.getId(), pageableSignature(pageable)),
            () -> resourceMapper.toPageResponse(
                resourceRepository.findByFolder_IdAndVault_Workspace_Id(folder.getId(), workspaceId, pageable)
            )
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<ResourceResponse> search(
        String keyword,
        ResourceType type,
        UUID tagId,
        UUID vaultId,
        UUID folderId,
        Boolean rootOnly,
        Boolean favorite,
        org.springframework.data.domain.Pageable pageable
    ) {
        com.linkvault.workspaces.entity.Workspace workspace = workspaceService.getDefaultWorkspaceForCurrentUser();
        return search(workspace.getId(), keyword, type, tagId, vaultId, folderId, rootOnly, favorite, pageable);
    }

    @Transactional(readOnly = true)
    public PageResponse<ResourceResponse> search(
        UUID workspaceId,
        String keyword,
        ResourceType type,
        UUID tagId,
        UUID vaultId,
        UUID folderId,
        Boolean rootOnly,
        Boolean favorite,
        org.springframework.data.domain.Pageable pageable
    ) {
        return cachedResourcePage(
            workspaceId,
            RedisKeys.pageSignature("search", cleanKeyword(keyword), type, tagId, vaultId, folderId, rootOnly, favorite, pageableSignature(pageable)),
            () -> resourceMapper.toPageResponse(
                resourceSearchService.search(workspaceId, keyword, type, tagId, vaultId, folderId, rootOnly, favorite, pageable)
            )
        );
    }

    @Transactional(readOnly = true)
    public ResourceResponse getResourceResponse(UUID id) {
        Resource resource = getResource(id);
        return cachedResourceDetail(resource);
    }

    @Transactional(readOnly = true)
    public ResourceResponse getResourceResponse(UUID workspaceId, UUID id) {
        Resource resource = getResource(workspaceId, id);
        return cachedResourceDetail(resource);
    }

    @Transactional
    public ResourceResponse createInVault(UUID vaultId, ResourceRequest request) {
        Vault vault = vaultService.getVaultForWrite(vaultId);
        return create(request, vault, null);
    }

    @Transactional
    public ResourceResponse createInVault(UUID workspaceId, UUID vaultId, ResourceRequest request) {
        Vault vault = vaultService.getVaultForWrite(workspaceId, vaultId);
        return create(request, vault, null);
    }

    @Transactional
    public ResourceResponse createInFolder(UUID folderId, ResourceRequest request) {
        Folder folder = folderService.getFolderForWrite(folderId);
        return create(request, folder.getVault(), folder);
    }

    @Transactional
    public ResourceResponse createInFolder(UUID workspaceId, UUID folderId, ResourceRequest request) {
        Folder folder = folderService.getFolderForWrite(workspaceId, folderId);
        return create(request, folder.getVault(), folder);
    }

    @Transactional
    public ResourceResponse update(UUID id, ResourceRequest request) {
        Resource resource = getResourceForWrite(id);
        String previousUrl = resource.getUrl();

        if (request.resourceType() == ResourceType.FILE && resource.getResourceType() != ResourceType.FILE) {
            throw new BadRequestException("Use upload endpoint to create file resources");
        }

        if (resource.getResourceType() == ResourceType.FILE && request.resourceType() != ResourceType.FILE) {
            clearFileFields(resource);
        }

        applyRequest(resource, request, resource.getVault(), resource.getFolder());
        boolean dispatchPreview = refreshPreviewIfNeeded(resource, previousUrl, false);
        Resource savedResource = resourceRepository.saveAndFlush(resource);
        if (dispatchPreview) {
            dispatchLinkPreviewEvent(savedResource, false);
        }
        auditLogService.recordAsync(savedResource.getVault().getWorkspace(), userContextService.getCurrentUser(), "resource.updated", "RESOURCE", savedResource.getId());
        invalidateResourceCaches(savedResource);
        return resourceMapper.toResponse(savedResource);
    }

    @Transactional
    public ResourceResponse update(UUID workspaceId, UUID id, ResourceRequest request) {
        Resource resource = getResourceForWrite(workspaceId, id);
        String previousUrl = resource.getUrl();

        if (request.resourceType() == ResourceType.FILE && resource.getResourceType() != ResourceType.FILE) {
            throw new BadRequestException("Use upload endpoint to create file resources");
        }

        if (resource.getResourceType() == ResourceType.FILE && request.resourceType() != ResourceType.FILE) {
            clearFileFields(resource);
        }

        applyRequest(resource, request, resource.getVault(), resource.getFolder());
        boolean dispatchPreview = refreshPreviewIfNeeded(resource, previousUrl, false);
        Resource savedResource = resourceRepository.saveAndFlush(resource);
        if (dispatchPreview) {
            dispatchLinkPreviewEvent(savedResource, false);
        }
        auditLogService.recordAsync(savedResource.getVault().getWorkspace(), userContextService.getCurrentUser(), "resource.updated", "RESOURCE", savedResource.getId());
        invalidateResourceCaches(savedResource);
        return resourceMapper.toResponse(savedResource);
    }

    @Transactional
    public ResourceResponse uploadFileToVault(
        UUID vaultId,
        String title,
        String description,
        MultipartFile file
    ) {
        Vault vault = vaultService.getVaultForWrite(vaultId);
        return uploadFile(vault, null, title, description, file);
    }

    @Transactional
    public ResourceResponse uploadFileToVault(
        UUID workspaceId,
        UUID vaultId,
        String title,
        String description,
        MultipartFile file
    ) {
        Vault vault = vaultService.getVaultForWrite(workspaceId, vaultId);
        return uploadFile(vault, null, title, description, file);
    }

    @Transactional
    public ResourceResponse uploadFileToFolder(
        UUID folderId,
        String title,
        String description,
        MultipartFile file
    ) {
        Folder folder = folderService.getFolderForWrite(folderId);
        return uploadFile(folder.getVault(), folder, title, description, file);
    }

    @Transactional
    public ResourceResponse uploadFileToFolder(
        UUID workspaceId,
        UUID folderId,
        String title,
        String description,
        MultipartFile file
    ) {
        Folder folder = folderService.getFolderForWrite(workspaceId, folderId);
        return uploadFile(folder.getVault(), folder, title, description, file);
    }

    @Transactional
    public void delete(UUID id) {
        Resource resource = getResourceForWrite(id);
        auditLogService.recordAsync(resource.getVault().getWorkspace(), userContextService.getCurrentUser(), "resource.deleted", "RESOURCE", resource.getId());
        resourceCleanupService.delete(resource);
        invalidateResourceCaches(resource);
    }

    @Transactional
    public void delete(UUID workspaceId, UUID id) {
        Resource resource = getResourceForWrite(workspaceId, id);
        auditLogService.recordAsync(resource.getVault().getWorkspace(), userContextService.getCurrentUser(), "resource.deleted", "RESOURCE", resource.getId());
        resourceCleanupService.delete(resource);
        invalidateResourceCaches(resource);
    }

    @Transactional
    public ResourceResponse toggleFavorite(UUID id) {
        Resource resource = getResourceForWrite(id);
        resource.setIsFavorite(!Boolean.TRUE.equals(resource.getIsFavorite()));
        Resource savedResource = resourceRepository.save(resource);
        auditLogService.recordAsync(savedResource.getVault().getWorkspace(), userContextService.getCurrentUser(), "resource.favorite_toggled", "RESOURCE", savedResource.getId());
        invalidateResourceCaches(savedResource);
        return resourceMapper.toResponse(savedResource);
    }

    @Transactional
    public ResourceResponse toggleFavorite(UUID workspaceId, UUID id) {
        Resource resource = getResourceForWrite(workspaceId, id);
        resource.setIsFavorite(!Boolean.TRUE.equals(resource.getIsFavorite()));
        Resource savedResource = resourceRepository.save(resource);
        auditLogService.recordAsync(savedResource.getVault().getWorkspace(), userContextService.getCurrentUser(), "resource.favorite_toggled", "RESOURCE", savedResource.getId());
        invalidateResourceCaches(savedResource);
        return resourceMapper.toResponse(savedResource);
    }

    @Transactional
    public ResourceResponse toggleArchive(UUID id) {
        Resource resource = getResourceForWrite(id);
        resource.setIsArchived(!Boolean.TRUE.equals(resource.getIsArchived()));
        Resource savedResource = resourceRepository.save(resource);
        auditLogService.recordAsync(savedResource.getVault().getWorkspace(), userContextService.getCurrentUser(), "resource.archived_toggled", "RESOURCE", savedResource.getId());
        invalidateResourceCaches(savedResource);
        return resourceMapper.toResponse(savedResource);
    }

    @Transactional
    public ResourceResponse toggleArchive(UUID workspaceId, UUID id) {
        Resource resource = getResourceForWrite(workspaceId, id);
        resource.setIsArchived(!Boolean.TRUE.equals(resource.getIsArchived()));
        Resource savedResource = resourceRepository.save(resource);
        auditLogService.recordAsync(savedResource.getVault().getWorkspace(), userContextService.getCurrentUser(), "resource.archived_toggled", "RESOURCE", savedResource.getId());
        invalidateResourceCaches(savedResource);
        return resourceMapper.toResponse(savedResource);
    }

    @Transactional
    public ResourceResponse recordView(UUID id) {
        Resource resource = getResource(id);
        User user = userContextService.getCurrentUser();

        ResourceView view = new ResourceView();
        view.setResource(resource);
        view.setUser(user);
        view.setViewedAt(Instant.now());
        resourceViewRepository.save(view);

        return resourceMapper.toResponse(resource);
    }

    @Transactional
    public ResourceResponse recordView(UUID workspaceId, UUID id) {
        Resource resource = getResource(workspaceId, id);
        User user = userContextService.getCurrentUser();

        ResourceView view = new ResourceView();
        view.setResource(resource);
        view.setUser(user);
        view.setViewedAt(Instant.now());
        resourceViewRepository.save(view);

        return resourceMapper.toResponse(resource);
    }

    @Transactional
    public ResourceResponse attachTag(UUID resourceId, UUID tagId) {
        Resource resource = getResourceForWrite(resourceId);
        Tag tag = tagService.getTag(resourceWorkspaceId(resource), tagId);

        if (!resourceTagRepository.existsByResource_IdAndTag_Id(resourceId, tagId)) {
            ResourceTag link = new ResourceTag();
            link.setResource(resource);
            link.setTag(tag);
            resourceTagRepository.save(link);
        }

        invalidateResourceCaches(resource);
        tagService.invalidateTagCaches(resourceWorkspaceId(resource));
        return resourceMapper.toResponse(resource);
    }

    @Transactional
    public ResourceResponse attachTag(UUID workspaceId, UUID resourceId, UUID tagId) {
        Resource resource = getResourceForWrite(workspaceId, resourceId);
        Tag tag = tagService.getTag(workspaceId, tagId);

        if (!tag.getWorkspace().getId().equals(resourceWorkspaceId(resource))) {
            throw new BadRequestException("Tag must belong to the same workspace as the resource");
        }

        if (!resourceTagRepository.existsByResource_IdAndTag_Id(resourceId, tagId)) {
            ResourceTag link = new ResourceTag();
            link.setResource(resource);
            link.setTag(tag);
            resourceTagRepository.save(link);
        }

        invalidateResourceCaches(resource);
        tagService.invalidateTagCaches(resourceWorkspaceId(resource));
        return resourceMapper.toResponse(resource);
    }

    @Transactional
    public ResourceResponse detachTag(UUID resourceId, UUID tagId) {
        Resource resource = getResourceForWrite(resourceId);
        tagService.getTag(resourceWorkspaceId(resource), tagId);
        resourceTagRepository.deleteByResource_IdAndTag_Id(resourceId, tagId);
        invalidateResourceCaches(resource);
        tagService.invalidateTagCaches(resourceWorkspaceId(resource));
        return resourceMapper.toResponse(resource);
    }

    @Transactional
    public ResourceResponse detachTag(UUID workspaceId, UUID resourceId, UUID tagId) {
        Resource resource = getResourceForWrite(workspaceId, resourceId);
        tagService.getTag(workspaceId, tagId);
        resourceTagRepository.deleteByResource_IdAndTag_Id(resourceId, tagId);
        invalidateResourceCaches(resource);
        tagService.invalidateTagCaches(resourceWorkspaceId(resource));
        return resourceMapper.toResponse(resource);
    }

    @Transactional
    public ResourceResponse refreshLinkPreview(UUID id) {
        Resource resource = getResourceForWrite(id);
        if (resource.getResourceType() != ResourceType.LINK) {
            throw new BadRequestException("Only link resources can refresh link preview");
        }

        boolean dispatchPreview = prepareLinkPreviewAsync(resource, true, false);
        Resource savedResource = resourceRepository.saveAndFlush(resource);
        if (dispatchPreview) {
            dispatchLinkPreviewEvent(savedResource, true);
        }
        invalidateResourceCaches(savedResource);
        return resourceMapper.toResponse(savedResource);
    }

    @Transactional
    public ResourceResponse refreshLinkPreview(UUID workspaceId, UUID id) {
        Resource resource = getResourceForWrite(workspaceId, id);
        if (resource.getResourceType() != ResourceType.LINK) {
            throw new BadRequestException("Only link resources can refresh link preview");
        }

        boolean dispatchPreview = prepareLinkPreviewAsync(resource, true, false);
        Resource savedResource = resourceRepository.saveAndFlush(resource);
        if (dispatchPreview) {
            dispatchLinkPreviewEvent(savedResource, true);
        }
        invalidateResourceCaches(savedResource);
        return resourceMapper.toResponse(savedResource);
    }

    @Transactional
    public void processLinkPreview(UUID resourceId, String expectedUrl, boolean force) {
        Resource resource = resourceRepository.findById(resourceId).orElse(null);
        if (resource == null || resource.getResourceType() != ResourceType.LINK) {
            return;
        }
        
        if (expectedUrl != null && !expectedUrl.equals(resource.getUrl())) {
            org.slf4j.LoggerFactory.getLogger(ResourceService.class).info("Skipping stale link preview for resourceId={}, expectedUrl={}, actualUrl={}", resourceId, expectedUrl, resource.getUrl());
            return;
        }

        applyLinkPreview(resource, force, false);
        Resource savedResource = resourceRepository.save(resource);
        invalidateResourceCaches(savedResource);
        org.slf4j.LoggerFactory.getLogger(ResourceService.class).info("Processed async link preview for resourceId={}", resourceId);
    }

    @Transactional
    public void markLinkPreviewFailed(UUID resourceId, String error) {
        Resource resource = resourceRepository.findById(resourceId).orElse(null);
        if (resource == null || resource.getResourceType() != ResourceType.LINK) {
            return;
        }
        resource.setPreviewStatus("FAILED");
        resource.setPreviewError(error != null && error.length() > 1000 ? error.substring(0, 997) + "..." : error);
        Resource savedResource = resourceRepository.save(resource);
        invalidateResourceCaches(savedResource);
    }

    @Transactional(readOnly = true)
    public ResourcePreviewResponse preview(UUID id) {
        return resourcePreviewService.preview(getResource(id));
    }

    @Transactional(readOnly = true)
    public ResourcePreviewResponse preview(UUID workspaceId, UUID id) {
        return resourcePreviewService.preview(getResource(workspaceId, id));
    }

    @Transactional(readOnly = true)
    public ResourceFileContent fileContent(UUID id) {
        return resourcePreviewService.fileContent(getResource(id));
    }

    @Transactional(readOnly = true)
    public ResourceFileContent fileContent(UUID workspaceId, UUID id) {
        return resourcePreviewService.fileContent(getResource(workspaceId, id));
    }

    @Transactional(readOnly = true)
    public DocumentPreviewResponse documentPreview(UUID id) {
        return resourcePreviewService.documentPreview(getResource(id));
    }

    @Transactional(readOnly = true)
    public DocumentPreviewResponse documentPreview(UUID workspaceId, UUID id) {
        return resourcePreviewService.documentPreview(getResource(workspaceId, id));
    }

    @Transactional(readOnly = true)
    public Resource getResource(UUID id) {
        Resource resource = resourceRepository.findById(id)
            .orElseThrow(() -> new NotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Resource not found"));
        workspaceService.requireMember(resourceWorkspaceId(resource));
        return resource;
    }

    @Transactional(readOnly = true)
    public Resource getResource(UUID workspaceId, UUID id) {
        workspaceService.requireMember(workspaceId);
        return resourceRepository.findByIdAndVault_Workspace_Id(id, workspaceId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Resource not found"));
    }

    @Transactional(readOnly = true)
    public Resource getResourceForWrite(UUID id) {
        Resource resource = resourceRepository.findById(id)
            .orElseThrow(() -> new NotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Resource not found"));
        workspaceService.requireCanWrite(resourceWorkspaceId(resource));
        return resource;
    }

    @Transactional(readOnly = true)
    public Resource getResourceForWrite(UUID workspaceId, UUID id) {
        workspaceService.requireCanWrite(workspaceId);
        return resourceRepository.findByIdAndVault_Workspace_Id(id, workspaceId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Resource not found"));
    }


    private PageResponse<ResourceResponse> cachedResourcePage(
        UUID workspaceId,
        String signature,
        java.util.function.Supplier<PageResponse<ResourceResponse>> loader
    ) {
        workspaceService.requireMember(workspaceId);
        return redisCacheService.getOrLoad(
            RedisKeys.workspaceResources(workspaceId, signature),
            redisProperties.getCache().getResourceListTtl(),
            RESOURCE_PAGE_TYPE,
            loader
        );
    }

    private ResourceResponse cachedResourceDetail(Resource resource) {
        return redisCacheService.getOrLoad(
            RedisKeys.resourceDetail(resource.getId()),
            redisProperties.getCache().getResourceDetailTtl(),
            ResourceResponse.class,
            () -> resourceMapper.toResponse(resource)
        );
    }

    private String pageableSignature(org.springframework.data.domain.Pageable pageable) {
        if (pageable == null) {
            return "unpaged";
        }
        return pageable.getPageNumber() + ":" + pageable.getPageSize() + ":" + pageable.getSort();
    }

    private String cleanKeyword(String keyword) {
        return keyword == null || keyword.isBlank() ? null : keyword.trim();
    }

    private UUID resourceWorkspaceId(Vault vault) {
        return vault.getWorkspace().getId();
    }

    private UUID resourceWorkspaceId(Folder folder) {
        return folder.getVault().getWorkspace().getId();
    }

    private void invalidateResourceCaches(Resource resource) {
        cacheInvalidationService.invalidateWorkspace(resourceWorkspaceId(resource));
        cacheInvalidationService.invalidateVault(resource.getVault().getId());
        cacheInvalidationService.invalidateResource(resource.getId());
        if (resource.getFolder() != null) {
            cacheInvalidationService.invalidateFolder(resource.getFolder().getId());
        }
    }

    private UUID resourceWorkspaceId(Resource resource) {
        return resource.getVault().getWorkspace().getId();
    }

    public ResourceResponse toResponse(Resource resource) {
        return resourceMapper.toResponse(resource);
    }

    public List<ResourceResponse> toResponses(List<Resource> resources) {
        return resourceMapper.toResponses(resources);
    }

    private ResourceResponse create(ResourceRequest request, Vault vault, Folder folder) {
        if (request.resourceType() == ResourceType.FILE) {
            throw new BadRequestException("Use upload endpoint to create file resources");
        }

        quotaService.requireCanCreateResource(vault.getWorkspace());

        Resource resource = new Resource();
        applyRequest(resource, request, vault, folder);
        boolean dispatchPreview = refreshPreviewIfNeeded(resource, null, false);
        Resource savedResource = resourceRepository.saveAndFlush(resource);
        if (dispatchPreview) {
            dispatchLinkPreviewEvent(savedResource, false);
        }
        auditLogService.recordAsync(vault.getWorkspace(), userContextService.getCurrentUser(), "resource.created", "RESOURCE", savedResource.getId());
        invalidateResourceCaches(savedResource);
        return resourceMapper.toResponse(savedResource);
    }

    private ResourceResponse uploadFile(
        Vault vault,
        Folder folder,
        String title,
        String description,
        MultipartFile file
    ) {
        quotaService.requireCanUpload(vault.getWorkspace(), file.getSize());
        FileValidationUtil.validateMagicNumber(file);
        StorageResult storage = storageService.upload(file);

        try {
            Resource resource = new Resource();
            resource.setVault(vault);
            resource.setFolder(folder);
            resource.setTitle(resolveTitle(title, storage.originalFilename()));
            resource.setDescription(trimToNull(description));
            resource.setResourceType(ResourceType.FILE);
            resource.setFileUrl(storage.secureUrl() == null ? storage.url() : storage.secureUrl());
            resource.setFileName(storage.originalFilename());
            resource.setFileSize(storage.size());
            resource.setMimeType(storage.mimeType());
            resource.setStorageProvider("CLOUDINARY");
            resource.setStorageKey(storage.publicId());

            Resource savedResource = resourceRepository.save(resource);
            auditLogService.recordAsync(vault.getWorkspace(), userContextService.getCurrentUser(), "resource.uploaded", "RESOURCE", savedResource.getId());
            invalidateResourceCaches(savedResource);
            return resourceMapper.toResponse(savedResource);
        } catch (RuntimeException exception) {
            try {
                storageService.delete(storage.publicId(), storage.mimeType(), storage.originalFilename());
            } catch (Exception cleanupException) {
                org.slf4j.LoggerFactory.getLogger(ResourceService.class)
                    .error("Failed to clean up storage file after DB save failure: " + storage.publicId(), cleanupException);
            }
            throw exception;
        }
    }

    private void applyRequest(Resource resource, ResourceRequest request, Vault vault, Folder folder) {
        validateRequest(request);

        resource.setVault(vault);
        resource.setFolder(folder);
        resource.setTitle(request.title().trim());
        resource.setDescription(trimToNull(request.description()));
        resource.setResourceType(request.resourceType());
        resource.setUrl(request.resourceType() == ResourceType.LINK ? linkPreviewService.normalizeUserUrl(request.url()) : null);
        resource.setContent(usesContent(request.resourceType()) ? trimToNull(request.content()) : null);
        resource.setCodeLanguage(request.resourceType() == ResourceType.SNIPPET ? trimToNull(request.codeLanguage()) : null);
        resource.setSourceName(request.resourceType() == ResourceType.LINK ? trimToNull(request.sourceName()) : null);
        resource.setThumbnailUrl(request.resourceType() == ResourceType.LINK ? trimToNull(request.thumbnailUrl()) : null);
        
        if (request.publicAccess() != null) {
            resource.setPublicAccess(request.publicAccess());
        }

        if (request.resourceType() != ResourceType.LINK) {
            clearLinkPreviewFields(resource);
        }
    }

    private void validateRequest(ResourceRequest request) {
        if (request.resourceType() == ResourceType.LINK && isBlank(request.url())) {
            throw new BadRequestException("URL is required for link resources");
        }

        if (request.resourceType() == ResourceType.NOTE && isBlank(request.content())) {
            throw new BadRequestException("Content is required for note resources");
        }

        if (request.resourceType() == ResourceType.SNIPPET) {
            if (isBlank(request.content())) {
                throw new BadRequestException("Content is required for snippet resources");
            }
            if (isBlank(request.codeLanguage())) {
                throw new BadRequestException("Code language is required for snippet resources");
            }
        }

        if (request.resourceType() == ResourceType.FILE) {
            throw new BadRequestException("File resources cannot be created or updated via this endpoint");
        }
    }

    private String resolveTitle(String title, String originalFilename) {
        if (!isBlank(title)) {
            return title.trim();
        }
        if (!isBlank(originalFilename)) {
            return originalFilename.trim();
        }
        throw new BadRequestException("Title or file name is required");
    }

    private boolean usesContent(ResourceType type) {
        return type == ResourceType.NOTE || type == ResourceType.SNIPPET;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String trimToNull(String value) {
        if (isBlank(value)) {
            return null;
        }
        return value.trim();
    }

    private void clearFileFields(Resource resource) {
        resource.setFileUrl(null);
        resource.setFileName(null);
        resource.setFileSize(null);
        resource.setMimeType(null);
        resource.setStorageProvider(null);
        resource.setStorageKey(null);
    }

    private boolean refreshPreviewIfNeeded(Resource resource, String previousUrl, boolean force) {
        if (resource.getResourceType() != ResourceType.LINK) {
            return false;
        }

        boolean urlChanged = previousUrl == null || !previousUrl.equals(resource.getUrl());
        if (!force && !urlChanged && isRecentPreview(resource)) {
            return false;
        }

        return prepareLinkPreviewAsync(resource, force, urlChanged);
    }

    private boolean prepareLinkPreviewAsync(Resource resource, boolean force, boolean clearOnFailure) {
        resource.setPreviewStatus("PENDING");
        resource.setPreviewError(null);
        if (clearOnFailure) {
            resource.setPreviewTitle(null);
            resource.setPreviewDescription(null);
            resource.setFaviconUrl(null);
            resource.setSiteName(null);
            resource.setCanonicalUrl(resource.getUrl());
            resource.setThumbnailUrl(null);
            if (isBlank(resource.getSourceName())) {
                resource.setSourceName(trimToNull(linkPreviewService.displayDomain(resource.getUrl())));
            }
        }
        return true;
    }

    private void dispatchLinkPreviewEvent(Resource savedResource, boolean force) {
        if (savedResource.getId() == null) {
            throw new IllegalStateException("Cannot dispatch link preview event for resource with null ID");
        }
        UUID folderId = savedResource.getFolder() != null ? savedResource.getFolder().getId() : null;
        LinkPreviewRequestedEvent event = new LinkPreviewRequestedEvent(
            savedResource.getId(),
            savedResource.getVault().getWorkspace().getId(),
            savedResource.getVault().getId(),
            folderId,
            savedResource.getUrl(),
            force,
            Instant.now()
        );
        
        // Save to transactional outbox
        outboxService.saveEvent("Resource", savedResource.getId(), "LinkPreviewRequestedEvent", event);
    }

    private boolean isRecentPreview(Resource resource) {
        Instant fetchedAt = resource.getPreviewFetchedAt();
        return fetchedAt != null && fetchedAt.plus(Duration.ofMinutes(30)).isAfter(Instant.now());
    }

    private void applyLinkPreview(Resource resource, boolean force, boolean clearOnFailure) {
        LinkPreviewResponse preview = linkPreviewService.fetch(resource.getUrl(), force);
        resource.setPreviewFetchedAt(preview.previewFetchedAt());
        resource.setPreviewStatus(preview.previewStatus());
        resource.setPreviewError(preview.previewError());

        if (preview.successful()) {
            resource.setPreviewTitle(trimToNull(preview.previewTitle()));
            resource.setPreviewDescription(trimToNull(preview.previewDescription()));
            resource.setFaviconUrl(trimToNull(preview.faviconUrl()));
            resource.setSiteName(trimToNull(preview.siteName()));
            resource.setCanonicalUrl(trimToNull(preview.canonicalUrl()));
            resource.setSourceName(firstNonBlank(preview.sourceName(), preview.domain(), resource.getSourceName()));
            resource.setThumbnailUrl(trimToNull(preview.thumbnailUrl()));
            return;
        }

        if (clearOnFailure) {
            resource.setPreviewTitle(null);
            resource.setPreviewDescription(null);
            resource.setFaviconUrl(null);
            resource.setSiteName(null);
            resource.setCanonicalUrl(resource.getUrl());
            resource.setThumbnailUrl(null);
        }

        if (isBlank(resource.getSourceName())) {
            resource.setSourceName(trimToNull(firstNonBlank(preview.domain(), linkPreviewService.displayDomain(resource.getUrl()))));
        }
    }

    private void clearLinkPreviewFields(Resource resource) {
        resource.setPreviewTitle(null);
        resource.setPreviewDescription(null);
        resource.setFaviconUrl(null);
        resource.setSiteName(null);
        resource.setCanonicalUrl(null);
        resource.setPreviewFetchedAt(null);
        resource.setPreviewStatus(null);
        resource.setPreviewError(null);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (!isBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }
}

