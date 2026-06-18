package com.linkvault.resources.controller;

import com.linkvault.common.enums.PublicAccess;
import com.linkvault.common.exception.BadRequestException;
import com.linkvault.common.exception.ErrorCode;
import com.linkvault.common.exception.NotFoundException;
import com.linkvault.common.exception.UnauthorizedException;
import com.linkvault.common.response.ApiResponse;
import com.linkvault.resources.dto.ResourceRequest;
import com.linkvault.resources.dto.ResourceResponse;
import com.linkvault.resources.entity.Resource;
import com.linkvault.resources.mapper.ResourceMapper;
import com.linkvault.resources.repository.ResourceRepository;
import com.linkvault.resources.service.ResourceService;
import com.linkvault.vaults.entity.Vault;
import com.linkvault.vaults.repository.VaultRepository;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/resources")
public class PublicResourceController {

    private final ResourceRepository resourceRepository;
    private final ResourceService resourceService;
    private final ResourceMapper resourceMapper;
    private final VaultRepository vaultRepository;

    public PublicResourceController(
        ResourceRepository resourceRepository,
        ResourceService resourceService,
        ResourceMapper resourceMapper,
        VaultRepository vaultRepository
    ) {
        this.resourceRepository = resourceRepository;
        this.resourceService = resourceService;
        this.resourceMapper = resourceMapper;
        this.vaultRepository = vaultRepository;
    }

    private Resource requirePublicResource(UUID id, boolean requireEdit) {
        Resource resource = resourceRepository.findById(id)
            .orElseThrow(() -> new NotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Resource not found"));

        Vault vault = resource.getVault();

        // If the resource is specifically published
        boolean isResourcePublic = resource.getPublicAccess() != PublicAccess.PRIVATE;
        boolean isVaultPublic = vault.getPublicAccess() != PublicAccess.PRIVATE;

        if (!isResourcePublic && !isVaultPublic) {
            throw new UnauthorizedException("This resource is private");
        }

        if (requireEdit) {
            boolean canEditResource = resource.getPublicAccess() == PublicAccess.EDIT;
            boolean canEditVault = vault.getPublicAccess() == PublicAccess.EDIT;
            
            if (!canEditResource && !canEditVault) {
                throw new UnauthorizedException("You do not have permission to edit this resource");
            }
        }

        return resource;
    }

    @GetMapping("/{id}")
    public ApiResponse<ResourceResponse> getPublicResource(@PathVariable UUID id) {
        Resource resource = requirePublicResource(id, false);
        return ApiResponse.success("Resource loaded", resourceMapper.toResponse(resource));
    }

    @PutMapping("/{id}")
    public ApiResponse<ResourceResponse> updatePublicResource(
        @PathVariable UUID id,
        @Valid @RequestBody ResourceRequest request
    ) {
        // Only checking if the individual resource or its parent vault is editable
        requirePublicResource(id, true);
        
        // We can just use the regular ResourceService update method which will save it,
        // BUT ResourceService.update calls getResourceForWrite which checks workspace permissions.
        // So we cannot reuse resourceService.update here. We must update manually.
        
        Resource resource = requirePublicResource(id, true);
        
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
        return ApiResponse.success("Resource updated", resourceMapper.toResponse(savedResource));
    }
}
