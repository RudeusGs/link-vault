package com.linkvault.resources.controller;

import com.linkvault.common.enums.PublicAccess;
import com.linkvault.common.exception.BadRequestException;
import com.linkvault.common.exception.ErrorCode;
import com.linkvault.common.exception.NotFoundException;
import com.linkvault.common.exception.UnauthorizedException;
import com.linkvault.common.response.ApiResponse;
import com.linkvault.common.redis.RedisCacheInvalidationService;
import com.linkvault.resources.dto.PublicResourceResponse;
import com.linkvault.resources.dto.ResourceRequest;
import com.linkvault.resources.entity.Resource;
import com.linkvault.resources.mapper.ResourceMapper;
import com.linkvault.resources.repository.ResourceRepository;
import com.linkvault.resources.service.ResourcePreviewService;
import com.linkvault.vaults.entity.Vault;
import jakarta.validation.Valid;
import com.linkvault.sharing.service.ShareLinkService;
import java.util.UUID;
import com.linkvault.resources.dto.ResourceFileContent;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/resources")
public class PublicResourceController {

    private final ResourceRepository resourceRepository;
    private final ResourcePreviewService resourcePreviewService;
    private final ResourceMapper resourceMapper;
    private final RedisCacheInvalidationService cacheInvalidationService;
    private final ShareLinkService shareLinkService;

    public PublicResourceController(
        ResourceRepository resourceRepository,
        ResourcePreviewService resourcePreviewService,
        ResourceMapper resourceMapper,
        RedisCacheInvalidationService cacheInvalidationService,
        ShareLinkService shareLinkService
    ) {
        this.resourceRepository = resourceRepository;
        this.resourcePreviewService = resourcePreviewService;
        this.resourceMapper = resourceMapper;
        this.cacheInvalidationService = cacheInvalidationService;
        this.shareLinkService = shareLinkService;
    }

    private Resource requirePublicResource(UUID id, boolean requireEdit, String shareToken) {
        Resource resource = resourceRepository.findById(id)
            .orElseThrow(() -> new NotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Resource not found"));

        if (shareToken != null && !shareToken.isBlank()) {
            shareLinkService.validateAndRecordAccess(shareToken, "RESOURCE", id, requireEdit);
            return resource;
        }

        Vault vault = resource.getVault();

        // If the resource is specifically published
        boolean isResourcePublic = resource.getPublicAccess() != PublicAccess.PRIVATE;
        boolean isVaultPublic = vault.getPublicAccess() != PublicAccess.PRIVATE;

        if (!isResourcePublic && !isVaultPublic) {
            throw new UnauthorizedException("This resource is private");
        }

        if (requireEdit) {
            throw new UnauthorizedException("Editing requires a secure share token");
        }

        return resource;
    }

    @GetMapping("/{id}")
    public ApiResponse<PublicResourceResponse> getPublicResource(
        @PathVariable UUID id,
        @RequestParam(name = "share_token", required = false) String shareToken
    ) {
        Resource resource = requirePublicResource(id, false, shareToken);
        return ApiResponse.success("Resource loaded", resourceMapper.toPublicResponse(resource));
    }

    @PutMapping("/{id}")
    public ApiResponse<PublicResourceResponse> updatePublicResource(
        @PathVariable UUID id,
        @RequestParam(name = "share_token", required = false) String shareToken,
        @Valid @RequestBody ResourceRequest request
    ) {
        // Only checking if the individual resource or its parent vault is editable
        requirePublicResource(id, true, shareToken);
        
        // We can just use the regular ResourceService update method which will save it,
        // BUT ResourceService.update calls getResourceForWrite which checks workspace permissions.
        // So we cannot reuse resourceService.update here. We must update manually.
        
        Resource resource = requirePublicResource(id, true, shareToken);
        
        if (request.resourceType() != resource.getResourceType()) {
            throw new BadRequestException("Cannot change resource type");
        }
        
        resource.setTitle(request.title().trim());
        resource.setDescription(request.description() != null ? request.description().trim() : null);
        resource.setUrl(request.url() != null ? request.url().trim() : null);
        resource.setContent(request.content() != null ? request.content().trim() : null);
        resource.setCodeLanguage(request.codeLanguage() != null ? request.codeLanguage().trim() : null);
        resource.setSourceName(request.sourceName() != null ? request.sourceName().trim() : null);
        resource.setThumbnailUrl(request.thumbnailUrl() != null ? request.thumbnailUrl().trim() : null);
        
        Resource savedResource = resourceRepository.save(resource);
        cacheInvalidationService.invalidateWorkspace(savedResource.getVault().getWorkspace().getId());
        cacheInvalidationService.invalidateVault(savedResource.getVault().getId());
        cacheInvalidationService.invalidateResource(savedResource.getId());
        if (savedResource.getFolder() != null) {
            cacheInvalidationService.invalidateFolder(savedResource.getFolder().getId());
        }
        return ApiResponse.success("Resource updated", resourceMapper.toPublicResponse(savedResource));
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<byte[]> getPublicFile(
        @PathVariable UUID id,
        @RequestParam(name = "share_token", required = false) String shareToken
    ) {
        Resource resource = requirePublicResource(id, false, shareToken);
        ResourceFileContent file = resourcePreviewService.fileContent(resource);
        MediaType mediaType = MediaType.parseMediaType(file.mimeType());
        ContentDisposition disposition = ContentDisposition.inline()
            .filename(file.fileName() == null ? "resource-file" : file.fileName())
            .build();

        return ResponseEntity.ok()
            .contentType(mediaType)
            .cacheControl(CacheControl.noStore())
            .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
            .body(file.content());
    }
}

