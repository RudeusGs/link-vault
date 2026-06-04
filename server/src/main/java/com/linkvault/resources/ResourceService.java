package com.linkvault.resources;

import com.linkvault.common.exception.BadRequestException;
import com.linkvault.common.exception.ForbiddenException;
import com.linkvault.common.exception.NotFoundException;
import com.linkvault.folders.Folder;
import com.linkvault.folders.FolderService;
import com.linkvault.storage.StorageFile;
import com.linkvault.storage.StorageResult;
import com.linkvault.storage.StorageService;
import com.linkvault.tags.Tag;
import com.linkvault.tags.TagResponse;
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
import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
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
    private final EntityManager entityManager;

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
        EntityManager entityManager
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
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public List<ResourceResponse> listAll() {
        return search(null, null, null, null, null, null, null);
    }

    @Transactional(readOnly = true)
    public List<ResourceResponse> listByVault(UUID vaultId) {
        vaultService.getVault(vaultId);
        return toResponses(resourceRepository.findByVault_IdOrderByCreatedAtDesc(vaultId));
    }

    @Transactional(readOnly = true)
    public List<ResourceResponse> listByFolder(UUID folderId) {
        Folder folder = folderService.getFolder(folderId);
        return toResponses(resourceRepository.findByFolder_IdOrderByCreatedAtDesc(folder.getId()));
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
        User user = userContextService.getCurrentUser();
        Vault vault = vaultId == null ? null : vaultService.getVault(vaultId);
        Folder folder = folderId == null ? null : folderService.getFolder(folderId);
        if (tagId != null) {
            tagService.getTag(tagId);
        }
        if (vault != null && folder != null && !folder.getVault().getId().equals(vault.getId())) {
            throw new BadRequestException("Folder must belong to the selected vault");
        }

        return toResponses(searchResources(
            user.getId(),
            cleanKeyword(keyword),
            type,
            tagId,
            vaultId,
            folderId,
            rootOnly,
            favorite
        ));
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

    @Transactional(readOnly = true)
    public ResourceResponse getResourceResponse(UUID id) {
        return toResponse(getResource(id));
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
        return toResponse(resourceRepository.save(resource));
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
        User user = userContextService.getCurrentUser();

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
        tagService.getTag(tagId);
        resourceTagRepository.deleteByResource_IdAndTag_Id(resourceId, tagId);
        return toResponse(resource);
    }

    @Transactional
    public ResourceResponse refreshLinkPreview(UUID id) {
        Resource resource = getResource(id);
        if (resource.getResourceType() != ResourceType.LINK) {
            throw new BadRequestException("Only link resources can refresh link preview");
        }

        applyLinkPreview(resource, true, false);
        return toResponse(resourceRepository.save(resource));
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
            "/api/resources/" + resource.getId() + "/file",
            resource.getMimeType(),
            resource.getFileName(),
            supported,
            supported ? null : "Preview is not supported for this file type"
        );
    }

    @Transactional(readOnly = true)
    public ResourceFileContent fileContent(UUID id) {
        Resource resource = getResource(id);
        if (resource.getResourceType() != ResourceType.FILE) {
            throw new BadRequestException("Resource is not a file");
        }
        if (isBlank(resource.getFileUrl())) {
            throw new BadRequestException("File URL is missing");
        }

        StorageFile storageFile = storageService.download(resource.getFileUrl());
        String mimeType = resolveResponseMimeType(resource.getMimeType(), storageFile.mimeType(), resource.getFileName());
        return new ResourceFileContent(
            resource.getId(),
            resource.getFileName(),
            mimeType,
            storageFile.content()
        );
    }

    @Transactional(readOnly = true)
    public DocumentPreviewResponse documentPreview(UUID id) {
        Resource resource = getResource(id);
        if (resource.getResourceType() != ResourceType.FILE) {
            throw new BadRequestException("Resource is not a file");
        }

        String extension = extensionOf(resource.getFileName());
        if (!extension.equals("docx")) {
            return new DocumentPreviewResponse(
                resource.getId(),
                resource.getTitle(),
                resource.getFileName(),
                resource.getMimeType(),
                "",
                0,
                false,
                extension.equals("doc")
                    ? "Legacy .doc files cannot be rendered inline yet. Convert it to .docx for live preview."
                    : "Document preview is available for .docx files"
            );
        }

        ResourceFileContent file = fileContent(id);
        List<String> paragraphs = extractDocxParagraphs(file.content());
        String plainText = String.join("\n\n", paragraphs);

        return new DocumentPreviewResponse(
            resource.getId(),
            resource.getTitle(),
            resource.getFileName(),
            file.mimeType(),
            plainText,
            paragraphs.size(),
            true,
            plainText.isBlank() ? "The DOCX file did not contain readable body text" : null
        );
    }

    @Transactional(readOnly = true)
    public Resource getResource(UUID id) {
        Resource resource = resourceRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Resource not found"));
        ensureOwner(resource);
        return resource;
    }

    public ResourceResponse toResponse(Resource resource) {
        return toResponse(resource, tagsByResourceId(List.of(resource)));
    }

    public List<ResourceResponse> toResponses(List<Resource> resources) {
        Map<UUID, List<TagResponse>> tagsByResourceId = tagsByResourceId(resources);
        return resources.stream()
            .map(resource -> toResponse(resource, tagsByResourceId))
            .toList();
    }

    private ResourceResponse toResponse(Resource resource, Map<UUID, List<TagResponse>> tagsByResourceId) {
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
            resource.getPreviewTitle(),
            resource.getPreviewDescription(),
            resource.getFaviconUrl(),
            resource.getSiteName(),
            resource.getCanonicalUrl(),
            resource.getPreviewFetchedAt(),
            resource.getPreviewStatus(),
            resource.getPreviewError(),
            resource.getIsFavorite(),
            resource.getIsArchived(),
            tagsByResourceId.getOrDefault(resource.getId(), List.of()),
            resource.getCreatedAt(),
            resource.getUpdatedAt()
        );
    }

    private Map<UUID, List<TagResponse>> tagsByResourceId(List<Resource> resources) {
        List<UUID> resourceIds = resources.stream()
            .map(Resource::getId)
            .filter(java.util.Objects::nonNull)
            .toList();

        if (resourceIds.isEmpty()) {
            return Map.of();
        }

        List<ResourceTag> resourceTags = resourceTagRepository.findByResource_IdIn(resourceIds);
        Map<UUID, Tag> uniqueTags = new LinkedHashMap<>();
        resourceTags.forEach(resourceTag -> uniqueTags.put(resourceTag.getTag().getId(), resourceTag.getTag()));

        Map<UUID, TagResponse> tagResponsesById = new HashMap<>();
        tagService.toResponses(List.copyOf(uniqueTags.values()))
            .forEach(tagResponse -> tagResponsesById.put(tagResponse.id(), tagResponse));

        Map<UUID, List<TagResponse>> result = new HashMap<>();
        resourceTags.forEach(resourceTag -> {
            UUID resourceId = resourceTag.getResource().getId();
            TagResponse tag = tagResponsesById.get(resourceTag.getTag().getId());
            if (tag == null) {
                return;
            }
            result.computeIfAbsent(resourceId, ignored -> new ArrayList<>())
                .add(tag);
        });
        return result;
    }

    private ResourceResponse create(ResourceRequest request, Vault vault, Folder folder) {
        if (request.resourceType() == ResourceType.FILE) {
            throw new BadRequestException("Use upload endpoint to create file resources");
        }

        Resource resource = new Resource();
        applyRequest(resource, request, vault, folder);
        refreshPreviewIfNeeded(resource, null, false);
        return toResponse(resourceRepository.save(resource));
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

        return toResponse(resourceRepository.save(resource));
    }

    private void applyRequest(Resource resource, ResourceRequest request, Vault vault, Folder folder) {
        validateRequest(request);

        resource.setVault(vault);
        resource.setFolder(folder);
        resource.setTitle(request.title().trim());
        resource.setDescription(trimToNull(request.description()));
        resource.setResourceType(request.resourceType());
        resource.setUrl(request.resourceType() == ResourceType.LINK ? normalizeUrl(request.url()) : null);
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
            throw new ForbiddenException("You do not have access to this resource");
        }
    }

    private String normalizeUrl(String value) {
        String url = trimToNull(value);
        if (url == null) {
            return null;
        }

        String lower = url.toLowerCase(Locale.ROOT);
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            url = "https://" + url;
        }

        return url;
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

    private boolean isPreviewSupported(String mimeType, String fileName) {
        String mime = mimeType == null ? "" : mimeType.toLowerCase(Locale.ROOT);
        String extension = extensionOf(fileName);

        if (mime.startsWith("image/") || imageExtensions().contains(extension)) {
            return true;
        }

        if (mime.startsWith("text/") || textPreviewExtensions().contains(extension)) {
            return true;
        }

        return mime.equals("application/pdf") || mime.equals("application/json") || extension.equals("pdf") || extension.equals("docx");
    }

    private List<String> imageExtensions() {
        return List.of("jpg", "jpeg", "png", "webp", "gif", "svg");
    }

    private List<String> textPreviewExtensions() {
        return List.of(
            "txt", "md", "csv", "json", "xml", "yaml", "yml", "log",
            "java", "kt", "py", "ts", "tsx", "js", "jsx", "html", "css", "scss",
            "sql", "sh", "ps1", "c", "cpp", "h", "hpp", "cs", "go", "rs", "php",
            "rb", "swift", "dart"
        );
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

    private String resolveResponseMimeType(String resourceMimeType, String storageMimeType, String fileName) {
        String mime = firstNonBlank(resourceMimeType, storageMimeType);
        if (!isBlank(mime) && !"application/octet-stream".equalsIgnoreCase(mime)) {
            return mime.toLowerCase(Locale.ROOT);
        }

        return switch (extensionOf(fileName)) {
            case "pdf" -> "application/pdf";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "doc" -> "application/msword";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "xls" -> "application/vnd.ms-excel";
            case "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "ppt" -> "application/vnd.ms-powerpoint";
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            case "gif" -> "image/gif";
            case "svg" -> "image/svg+xml";
            case "csv" -> "text/csv";
            case "json" -> "application/json";
            case "xml" -> "application/xml";
            default -> "application/octet-stream";
        };
    }

    private String firstNonBlank(String first, String second) {
        return isBlank(first) ? trimToNull(second) : first.trim();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (!isBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private List<String> extractDocxParagraphs(byte[] content) {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(content))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if ("word/document.xml".equals(entry.getName())) {
                    return parseDocxDocumentXml(zip);
                }
            }
        } catch (java.io.IOException | XMLStreamException exception) {
            throw new BadRequestException("Could not render DOCX preview");
        }

        throw new BadRequestException("DOCX document body was not found");
    }

    private List<String> parseDocxDocumentXml(ZipInputStream zip) throws XMLStreamException {
        XMLInputFactory factory = XMLInputFactory.newFactory();
        setXmlProperty(factory, XMLInputFactory.SUPPORT_DTD, false);
        setXmlProperty(factory, "javax.xml.stream.isSupportingExternalEntities", false);

        XMLStreamReader reader = factory.createXMLStreamReader(zip);
        List<String> paragraphs = new ArrayList<>();
        StringBuilder currentParagraph = new StringBuilder();
        boolean inParagraph = false;
        boolean inText = false;

        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                String name = reader.getLocalName();
                if ("p".equals(name)) {
                    inParagraph = true;
                    currentParagraph.setLength(0);
                } else if ("t".equals(name)) {
                    inText = true;
                } else if (inParagraph && "tab".equals(name)) {
                    currentParagraph.append('\t');
                } else if (inParagraph && "br".equals(name)) {
                    currentParagraph.append('\n');
                }
            } else if (event == XMLStreamConstants.CHARACTERS && inParagraph && inText) {
                currentParagraph.append(reader.getText());
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                String name = reader.getLocalName();
                if ("t".equals(name)) {
                    inText = false;
                } else if ("p".equals(name)) {
                    String paragraph = currentParagraph.toString().trim();
                    if (!paragraph.isBlank()) {
                        paragraphs.add(paragraph);
                    }
                    inParagraph = false;
                    inText = false;
                }
            }
        }

        return paragraphs;
    }

    private void setXmlProperty(XMLInputFactory factory, String property, boolean value) {
        try {
            factory.setProperty(property, value);
        } catch (IllegalArgumentException ignored) {
            // Some XML providers do not expose every hardening flag.
        }
    }
}
