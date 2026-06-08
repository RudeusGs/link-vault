package com.linkvault.resources;

import com.linkvault.common.exception.BadRequestException;
import com.linkvault.common.exception.ErrorCode;
import com.linkvault.common.exception.ForbiddenException;
import com.linkvault.common.exception.NotFoundException;
import com.linkvault.folders.Folder;
import com.linkvault.folders.FolderService;
import com.linkvault.storage.StorageResult;
import com.linkvault.storage.StorageService;
import com.linkvault.tags.Tag;
import com.linkvault.tags.TagService;
import com.linkvault.users.User;
import com.linkvault.users.UserContextService;
import com.linkvault.vaults.Vault;
import com.linkvault.vaults.VaultService;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ResourceService {

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
        ResourceCleanupService resourceCleanupService
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
    }

    @Transactional(readOnly = true)
    public List<ResourceResponse> listAll() {
        return search(null, null, null, null, null, null, null);
    }

    @Transactional(readOnly = true)
    public List<ResourceResponse> listByVault(UUID vaultId) {
        vaultService.getVault(vaultId);
        return resourceMapper.toResponses(resourceRepository.findByVault_IdOrderByCreatedAtDesc(vaultId));
    }

    @Transactional(readOnly = true)
    public List<ResourceResponse> listByFolder(UUID folderId) {
        Folder folder = folderService.getFolder(folderId);
        return resourceMapper.toResponses(resourceRepository.findByFolder_IdOrderByCreatedAtDesc(folder.getId()));
    }

    @Transactional(readOnly = true)
    public List<ResourceResponse> search(
        String keyword,
        ResourceType type,
        UUID tagId,
        UUID vaultId,
        UUID folderId,
        Boolean rootOnly,
        Boolean favorite
    ) {
        return resourceMapper.toResponses(
            resourceSearchService.search(keyword, type, tagId, vaultId, folderId, rootOnly, favorite)
        );
    }

    @Transactional(readOnly = true)
    public ResourceResponse getResourceResponse(UUID id) {
        return resourceMapper.toResponse(getResource(id));
    }

    @Transactional
    public ResourceResponse createInVault(UUID vaultId, ResourceRequest request) {
        Vault vault = vaultService.getVault(vaultId);
        return create(request, vault, null);
    }

    @Transactional
    public ResourceResponse createInFolder(UUID folderId, ResourceRequest request) {
        Folder folder = folderService.getFolder(folderId);
        return create(request, folder.getVault(), folder);
    }

    @Transactional
    public ResourceResponse update(UUID id, ResourceRequest request) {
        Resource resource = getResource(id);
        String previousUrl = resource.getUrl();

        if (request.resourceType() == ResourceType.FILE && resource.getResourceType() != ResourceType.FILE) {
            throw new BadRequestException("Use upload endpoint to create file resources");
        }

        if (resource.getResourceType() == ResourceType.FILE && request.resourceType() != ResourceType.FILE) {
            clearFileFields(resource);
        }

        applyRequest(resource, request, resource.getVault(), resource.getFolder());
        refreshPreviewIfNeeded(resource, previousUrl, false);
        return resourceMapper.toResponse(resourceRepository.save(resource));
    }

    @Transactional
    public ResourceResponse uploadFileToVault(
        UUID vaultId,
        String title,
        String description,
        MultipartFile file
    ) {
        Vault vault = vaultService.getVault(vaultId);
        return uploadFile(vault, null, title, description, file);
    }

    @Transactional
    public ResourceResponse uploadFileToFolder(
        UUID folderId,
        String title,
        String description,
        MultipartFile file
    ) {
        Folder folder = folderService.getFolder(folderId);
        return uploadFile(folder.getVault(), folder, title, description, file);
    }

    @Transactional
    public void delete(UUID id) {
        resourceCleanupService.delete(getResource(id));
    }

    @Transactional
    public ResourceResponse toggleFavorite(UUID id) {
        Resource resource = getResource(id);
        resource.setIsFavorite(!Boolean.TRUE.equals(resource.getIsFavorite()));
        return resourceMapper.toResponse(resourceRepository.save(resource));
    }

    @Transactional
    public ResourceResponse toggleArchive(UUID id) {
        Resource resource = getResource(id);
        resource.setIsArchived(!Boolean.TRUE.equals(resource.getIsArchived()));
        return resourceMapper.toResponse(resourceRepository.save(resource));
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
    public ResourceResponse attachTag(UUID resourceId, UUID tagId) {
        Resource resource = getResource(resourceId);
        Tag tag = tagService.getTag(tagId);

        if (!resourceTagRepository.existsByResource_IdAndTag_Id(resourceId, tagId)) {
            ResourceTag link = new ResourceTag();
            link.setResource(resource);
            link.setTag(tag);
            resourceTagRepository.save(link);
        }

        return resourceMapper.toResponse(resource);
    }

    @Transactional
    public ResourceResponse detachTag(UUID resourceId, UUID tagId) {
        Resource resource = getResource(resourceId);
        tagService.getTag(tagId);
        resourceTagRepository.deleteByResource_IdAndTag_Id(resourceId, tagId);
        return resourceMapper.toResponse(resource);
    }

    @Transactional
    public ResourceResponse refreshLinkPreview(UUID id) {
        Resource resource = getResource(id);
        if (resource.getResourceType() != ResourceType.LINK) {
            throw new BadRequestException("Only link resources can refresh link preview");
        }

        applyLinkPreview(resource, true, false);
        return resourceMapper.toResponse(resourceRepository.save(resource));
    }

    @Transactional(readOnly = true)
    public ResourcePreviewResponse preview(UUID id) {
        return resourcePreviewService.preview(getResource(id));
    }

    @Transactional(readOnly = true)
    public ResourceFileContent fileContent(UUID id) {
        return resourcePreviewService.fileContent(getResource(id));
    }

    @Transactional(readOnly = true)
    public DocumentPreviewResponse documentPreview(UUID id) {
        return resourcePreviewService.documentPreview(getResource(id));
    }

    @Transactional(readOnly = true)
    public Resource getResource(UUID id) {
        Resource resource = resourceRepository.findById(id)
            .orElseThrow(() -> new NotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Resource not found"));
        ensureOwner(resource);
        return resource;
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

        Resource resource = new Resource();
        applyRequest(resource, request, vault, folder);
        refreshPreviewIfNeeded(resource, null, false);
        return resourceMapper.toResponse(resourceRepository.save(resource));
    }

    private ResourceResponse uploadFile(
        Vault vault,
        Folder folder,
        String title,
        String description,
        MultipartFile file
    ) {
        StorageResult storage = storageService.upload(file);

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

        return resourceMapper.toResponse(resourceRepository.save(resource));
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

        if (request.resourceType() == ResourceType.SNIPPET && isBlank(request.content())) {
            throw new BadRequestException("Content is required for snippet resources");
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

    private void ensureOwner(Resource resource) {
        UUID currentUserId = userContextService.getCurrentUser().getId();
        if (!resource.getVault().getUser().getId().equals(currentUserId)) {
            throw new ForbiddenException(ErrorCode.RESOURCE_ACCESS_DENIED, "You do not have access to this resource");
        }
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

    private void refreshPreviewIfNeeded(Resource resource, String previousUrl, boolean force) {
        if (resource.getResourceType() != ResourceType.LINK) {
            return;
        }

        boolean urlChanged = previousUrl == null || !previousUrl.equals(resource.getUrl());
        if (!force && !urlChanged && isRecentPreview(resource)) {
            return;
        }

        applyLinkPreview(resource, force, urlChanged);
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
