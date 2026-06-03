package com.linkvault.resources;

import com.linkvault.common.exception.BadRequestException;
import com.linkvault.common.exception.NotFoundException;
import com.linkvault.folders.Folder;
import com.linkvault.folders.FolderService;
import com.linkvault.storage.StorageResult;
import com.linkvault.storage.StorageService;
import com.linkvault.tags.Tag;
import com.linkvault.tags.TagResponse;
import com.linkvault.tags.TagService;
import com.linkvault.users.User;
import com.linkvault.users.UserContextService;
import com.linkvault.vaults.Vault;
import com.linkvault.vaults.VaultService;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
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
    private final UserContextService userContextService;

    public ResourceService(
        ResourceRepository resourceRepository,
        ResourceTagRepository resourceTagRepository,
        ResourceViewRepository resourceViewRepository,
        VaultService vaultService,
        FolderService folderService,
        TagService tagService,
        StorageService storageService,
        UserContextService userContextService
    ) {
        this.resourceRepository = resourceRepository;
        this.resourceTagRepository = resourceTagRepository;
        this.resourceViewRepository = resourceViewRepository;
        this.vaultService = vaultService;
        this.folderService = folderService;
        this.tagService = tagService;
        this.storageService = storageService;
        this.userContextService = userContextService;
    }

    @Transactional(readOnly = true)
    public List<ResourceResponse> listAll() {
        return search(null, null, null, null, null, null);
    }

    @Transactional(readOnly = true)
    public List<ResourceResponse> listByVault(UUID vaultId) {
        return resourceRepository.findByVault_IdOrderByCreatedAtDesc(vaultId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<ResourceResponse> listByFolder(UUID folderId) {
        return resourceRepository.findByFolder_IdOrderByCreatedAtDesc(folderId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<ResourceResponse> search(
        String keyword,
        ResourceType type,
        UUID tagId,
        UUID vaultId,
        UUID folderId,
        Boolean favorite
    ) {
        return resourceRepository.search(cleanKeyword(keyword), type, tagId, vaultId, folderId, favorite).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public ResourceResponse getResourceResponse(UUID id) {
        return toResponse(getResource(id));
    }

    @Transactional
    public ResourceResponse create(ResourceRequest request) {
        if (request.resourceType() == ResourceType.FILE) {
            throw new BadRequestException("Use /api/resources/upload to create file resources");
        }

        Resource resource = new Resource();
        applyRequest(resource, request);
        return toResponse(resourceRepository.save(resource));
    }

    @Transactional
    public ResourceResponse update(UUID id, ResourceRequest request) {
        Resource resource = getResource(id);

        if (resource.getResourceType() == ResourceType.FILE && request.resourceType() != ResourceType.FILE) {
            clearFileFields(resource);
        }

        applyRequest(resource, request);
        return toResponse(resourceRepository.save(resource));
    }

    @Transactional
    public ResourceResponse uploadFile(
        UUID vaultId,
        UUID folderId,
        String title,
        String description,
        MultipartFile file
    ) {
        if (title == null || title.isBlank()) {
            throw new BadRequestException("Title is required");
        }

        Vault vault = vaultService.getVault(vaultId);
        Folder folder = resolveFolder(folderId, vault);
        StorageResult storage = storageService.upload(file);

        Resource resource = new Resource();
        resource.setVault(vault);
        resource.setFolder(folder);
        resource.setTitle(title.trim());
        resource.setDescription(description);
        resource.setResourceType(ResourceType.FILE);
        resource.setFileUrl(storage.secureUrl() == null ? storage.url() : storage.secureUrl());
        resource.setFileName(storage.originalFilename());
        resource.setFileSize(storage.size());
        resource.setMimeType(storage.mimeType());
        resource.setStorageProvider("CLOUDINARY");
        resource.setStorageKey(storage.publicId());

        return toResponse(resourceRepository.save(resource));
    }

    @Transactional
    public void delete(UUID id) {
        Resource resource = getResource(id);
        resourceTagRepository.deleteByResource_Id(id);
        resourceViewRepository.deleteByResource_Id(id);
        resourceRepository.delete(resource);
    }

    @Transactional
    public ResourceResponse toggleFavorite(UUID id) {
        Resource resource = getResource(id);
        resource.setIsFavorite(!Boolean.TRUE.equals(resource.getIsFavorite()));
        return toResponse(resourceRepository.save(resource));
    }

    @Transactional
    public ResourceResponse toggleArchive(UUID id) {
        Resource resource = getResource(id);
        resource.setIsArchived(!Boolean.TRUE.equals(resource.getIsArchived()));
        return toResponse(resourceRepository.save(resource));
    }

    @Transactional
    public ResourceResponse recordView(UUID id) {
        Resource resource = getResource(id);
        User user = userContextService.getDemoUser();

        ResourceView view = new ResourceView();
        view.setResource(resource);
        view.setUser(user);
        view.setViewedAt(Instant.now());
        resourceViewRepository.save(view);

        return toResponse(resource);
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

        return toResponse(resource);
    }

    @Transactional
    public ResourceResponse detachTag(UUID resourceId, UUID tagId) {
        Resource resource = getResource(resourceId);
        resourceTagRepository.deleteByResource_IdAndTag_Id(resourceId, tagId);
        return toResponse(resource);
    }

    @Transactional(readOnly = true)
    public ResourcePreviewResponse preview(UUID id) {
        Resource resource = getResource(id);

        if (resource.getResourceType() != ResourceType.FILE) {
            return new ResourcePreviewResponse(
                resource.getId(),
                resource.getResourceType(),
                resource.getTitle(),
                resource.getUrl(),
                resource.getMimeType(),
                resource.getFileName(),
                true,
                null
            );
        }

        boolean supported = isPreviewSupported(resource.getMimeType(), resource.getFileName());
        return new ResourcePreviewResponse(
            resource.getId(),
            resource.getResourceType(),
            resource.getTitle(),
            resource.getFileUrl(),
            resource.getMimeType(),
            resource.getFileName(),
            supported,
            supported ? null : "Preview is not supported for this file type"
        );
    }

    @Transactional(readOnly = true)
    public Resource getResource(UUID id) {
        return resourceRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Resource not found"));
    }

    public ResourceResponse toResponse(Resource resource) {
        List<TagResponse> tags = resource.getId() == null
            ? List.of()
            : resourceTagRepository.findByResource_Id(resource.getId()).stream()
                .map(ResourceTag::getTag)
                .map(tagService::toResponse)
                .toList();

        return new ResourceResponse(
            resource.getId(),
            resource.getVault().getId(),
            resource.getVault().getName(),
            resource.getFolder() == null ? null : resource.getFolder().getId(),
            resource.getFolder() == null ? null : resource.getFolder().getName(),
            resource.getTitle(),
            resource.getDescription(),
            resource.getResourceType(),
            resource.getUrl(),
            resource.getFileUrl(),
            resource.getFileName(),
            resource.getFileSize(),
            resource.getMimeType(),
            resource.getStorageProvider(),
            resource.getStorageKey(),
            resource.getContent(),
            resource.getCodeLanguage(),
            resource.getSourceName(),
            resource.getThumbnailUrl(),
            resource.getIsFavorite(),
            resource.getIsArchived(),
            tags,
            resource.getCreatedAt(),
            resource.getUpdatedAt()
        );
    }

    private void applyRequest(Resource resource, ResourceRequest request) {
        Vault vault = vaultService.getVault(request.vaultId());
        Folder folder = resolveFolder(request.folderId(), vault);

        validateRequest(request);

        resource.setVault(vault);
        resource.setFolder(folder);
        resource.setTitle(request.title().trim());
        resource.setDescription(request.description());
        resource.setResourceType(request.resourceType());
        resource.setUrl(request.resourceType() == ResourceType.LINK ? request.url() : null);
        resource.setContent(usesContent(request.resourceType()) ? request.content() : null);
        resource.setCodeLanguage(request.resourceType() == ResourceType.SNIPPET ? request.codeLanguage() : null);
        resource.setSourceName(request.resourceType() == ResourceType.LINK ? request.sourceName() : null);
        resource.setThumbnailUrl(request.resourceType() == ResourceType.LINK ? request.thumbnailUrl() : null);
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

    private Folder resolveFolder(UUID folderId, Vault vault) {
        if (folderId == null) {
            return null;
        }

        Folder folder = folderService.getFolder(folderId);
        if (!folder.getVault().getId().equals(vault.getId())) {
            throw new BadRequestException("Folder must belong to the selected vault");
        }

        return folder;
    }

    private boolean usesContent(ResourceType type) {
        return type == ResourceType.NOTE || type == ResourceType.SNIPPET;
    }

    private String cleanKeyword(String keyword) {
        return isBlank(keyword) ? null : keyword.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void clearFileFields(Resource resource) {
        resource.setFileUrl(null);
        resource.setFileName(null);
        resource.setFileSize(null);
        resource.setMimeType(null);
        resource.setStorageProvider(null);
        resource.setStorageKey(null);
    }

    private boolean isPreviewSupported(String mimeType, String fileName) {
        String mime = mimeType == null ? "" : mimeType.toLowerCase(Locale.ROOT);
        if (mime.startsWith("image/") || mime.startsWith("text/")) {
            return true;
        }

        if (mime.equals("application/pdf") || mime.equals("application/json")) {
            return true;
        }

        String extension = extensionOf(fileName);
        return List.of("txt", "md", "json", "java", "ts", "js", "html", "css").contains(extension);
    }

    private String extensionOf(String fileName) {
        if (fileName == null) {
            return "";
        }

        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return "";
        }

        return fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }
}
