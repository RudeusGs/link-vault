package com.linkvault.vaults.controller;

import com.linkvault.common.enums.PublicAccess;
import com.linkvault.common.exception.ErrorCode;
import com.linkvault.common.exception.NotFoundException;
import com.linkvault.common.exception.UnauthorizedException;
import com.linkvault.common.response.ApiResponse;
import com.linkvault.common.pagination.PageResponse;
import com.linkvault.resources.dto.ResourceResponse;
import com.linkvault.resources.mapper.ResourceMapper;
import com.linkvault.resources.repository.ResourceRepository;
import com.linkvault.vaults.dto.VaultRequest;
import com.linkvault.vaults.dto.VaultResponse;
import com.linkvault.vaults.entity.Vault;
import com.linkvault.vaults.repository.VaultRepository;
import com.linkvault.vaults.service.VaultService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/vaults")
public class PublicVaultController {

    private final VaultRepository vaultRepository;
    private final VaultService vaultService;
    private final ResourceRepository resourceRepository;
    private final ResourceMapper resourceMapper;

    public PublicVaultController(
        VaultRepository vaultRepository, 
        VaultService vaultService,
        ResourceRepository resourceRepository,
        ResourceMapper resourceMapper
    ) {
        this.vaultRepository = vaultRepository;
        this.vaultService = vaultService;
        this.resourceRepository = resourceRepository;
        this.resourceMapper = resourceMapper;
    }

    private Vault requirePublicVault(UUID id, boolean requireEdit) {
        Vault vault = vaultRepository.findById(id)
            .orElseThrow(() -> new NotFoundException(ErrorCode.VAULT_NOT_FOUND, "Vault not found"));
            
        if (vault.getPublicAccess() == PublicAccess.PRIVATE) {
            throw new UnauthorizedException("This vault is private");
        }
        
        if (requireEdit && vault.getPublicAccess() != PublicAccess.EDIT) {
            throw new UnauthorizedException("You do not have permission to edit this vault");
        }
        
        return vault;
    }

    @GetMapping("/{id}")
    public ApiResponse<VaultResponse> getPublicVault(@PathVariable UUID id) {
        Vault vault = requirePublicVault(id, false);
        return ApiResponse.success("Vault loaded", vaultService.toResponse(vault));
    }

    @GetMapping("/{id}/resources")
    public ApiResponse<PageResponse<ResourceResponse>> getPublicVaultResources(
        @PathVariable UUID id,
        Pageable pageable
    ) {
        Vault vault = requirePublicVault(id, false);
        return ApiResponse.success(
            "Resources loaded", 
            resourceMapper.toPageResponse(resourceRepository.findByVault_Id(vault.getId(), pageable))
        );
    }

    @PutMapping("/{id}")
    public ApiResponse<VaultResponse> updatePublicVault(
        @PathVariable UUID id,
        @Valid @RequestBody VaultRequest request
    ) {
        Vault vault = requirePublicVault(id, true);
        
        vault.setName(request.name().trim());
        vault.setDescription(request.description() != null ? request.description().trim() : null);
        vault.setIcon(request.icon() != null ? request.icon().trim() : null);
        vault.setColor(request.color() != null ? request.color().trim() : null);
        
        Vault savedVault = vaultRepository.save(vault);
        return ApiResponse.success("Vault updated", vaultService.toResponse(savedVault));
    }

    @PostMapping("/{id}/resources")
    public ApiResponse<ResourceResponse> createPublicResource(
        @PathVariable UUID id,
        @Valid @RequestBody com.linkvault.resources.dto.ResourceRequest request
    ) {
        Vault vault = requirePublicVault(id, true);
        
        com.linkvault.resources.entity.Resource resource = new com.linkvault.resources.entity.Resource();
        resource.setVault(vault);
        resource.setTitle(request.title().trim());
        resource.setDescription(request.description() != null ? request.description().trim() : null);
        resource.setResourceType(request.resourceType());
        resource.setUrl(request.url() != null ? request.url().trim() : null);
        resource.setContent(request.content() != null ? request.content().trim() : null);
        resource.setCodeLanguage(request.codeLanguage() != null ? request.codeLanguage().trim() : null);
        resource.setSourceName(request.sourceName() != null ? request.sourceName().trim() : null);
        resource.setThumbnailUrl(request.thumbnailUrl() != null ? request.thumbnailUrl().trim() : null);
        
        com.linkvault.resources.entity.Resource savedResource = resourceRepository.save(resource);
        return ApiResponse.success("Resource created", resourceMapper.toResponse(savedResource));
    }
}
