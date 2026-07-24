package com.linkvault.vaults.controller;

import com.linkvault.common.enums.PublicAccess;
import com.linkvault.common.exception.ErrorCode;
import com.linkvault.common.exception.NotFoundException;
import com.linkvault.common.exception.UnauthorizedException;
import com.linkvault.common.response.ApiResponse;
import com.linkvault.common.redis.RedisCacheInvalidationService;
import com.linkvault.common.pagination.PageResponse;
import com.linkvault.resources.dto.PublicResourceResponse;
import com.linkvault.resources.mapper.ResourceMapper;
import com.linkvault.resources.repository.ResourceRepository;
import com.linkvault.vaults.dto.VaultRequest;
import com.linkvault.vaults.dto.VaultResponse;
import com.linkvault.vaults.entity.Vault;
import com.linkvault.vaults.repository.VaultRepository;
import com.linkvault.vaults.service.VaultService;
import com.linkvault.sharing.service.ShareLinkService;
import com.linkvault.workspaces.service.QuotaService;
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
    private final RedisCacheInvalidationService cacheInvalidationService;
    private final ShareLinkService shareLinkService;
    private final QuotaService quotaService;

    public PublicVaultController(
        VaultRepository vaultRepository, 
        VaultService vaultService,
        ResourceRepository resourceRepository,
        ResourceMapper resourceMapper,
        RedisCacheInvalidationService cacheInvalidationService,
        ShareLinkService shareLinkService,
        QuotaService quotaService
    ) {
        this.vaultRepository = vaultRepository;
        this.vaultService = vaultService;
        this.resourceRepository = resourceRepository;
        this.resourceMapper = resourceMapper;
        this.cacheInvalidationService = cacheInvalidationService;
        this.shareLinkService = shareLinkService;
        this.quotaService = quotaService;
    }

    private Vault requirePublicVault(UUID id, boolean requireEdit, String shareToken) {
        Vault vault = vaultRepository.findById(id)
            .orElseThrow(() -> new NotFoundException(ErrorCode.VAULT_NOT_FOUND, "Vault not found"));
            
        if (shareToken != null && !shareToken.isBlank()) {
            shareLinkService.validateAndRecordAccess(shareToken, "VAULT", id, requireEdit);
            return vault;
        }
            
        if (vault.getPublicAccess() == PublicAccess.PRIVATE) {
            throw new UnauthorizedException("This vault is private");
        }
        
        if (requireEdit) {
            throw new UnauthorizedException("Editing requires a secure share token");
        }
        
        return vault;
    }

    @GetMapping("/{id}")
    public ApiResponse<VaultResponse> getPublicVault(
        @PathVariable UUID id,
        @RequestParam(name = "share_token", required = false) String shareToken
    ) {
        Vault vault = requirePublicVault(id, false, shareToken);
        return ApiResponse.success("Vault loaded", vaultService.toResponse(vault));
    }

    @GetMapping("/{id}/resources")
    public ApiResponse<PageResponse<PublicResourceResponse>> getPublicVaultResources(
        @PathVariable UUID id,
        @RequestParam(name = "share_token", required = false) String shareToken,
        Pageable pageable
    ) {
        Vault vault = requirePublicVault(id, false, shareToken);
        return ApiResponse.success(
            "Resources loaded", 
            resourceMapper.toPublicPageResponse(resourceRepository.findByVault_Id(vault.getId(), pageable))
        );
    }

    @PutMapping("/{id}")
    public ApiResponse<VaultResponse> updatePublicVault(
        @PathVariable UUID id,
        @RequestParam(name = "share_token", required = false) String shareToken,
        @Valid @RequestBody VaultRequest request
    ) {
        Vault vault = requirePublicVault(id, true, shareToken);
        
        vault.setName(request.name().trim());
        vault.setDescription(request.description() != null ? request.description().trim() : null);
        vault.setIcon(request.icon() != null ? request.icon().trim() : null);
        vault.setColor(request.color() != null ? request.color().trim() : null);
        
        Vault savedVault = vaultRepository.save(vault);
        cacheInvalidationService.invalidateWorkspace(savedVault.getWorkspace().getId());
        cacheInvalidationService.invalidateVault(savedVault.getId());
        return ApiResponse.success("Vault updated", vaultService.toResponse(savedVault));
    }

    @PostMapping("/{id}/resources")
    public ApiResponse<PublicResourceResponse> createPublicResource(
        @PathVariable UUID id,
        @RequestParam(name = "share_token", required = false) String shareToken,
        @Valid @RequestBody com.linkvault.resources.dto.ResourceRequest request
    ) {
        Vault vault = requirePublicVault(id, true, shareToken);
        
        quotaService.requireCanCreateResource(vault.getWorkspace());

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
        cacheInvalidationService.invalidateWorkspace(savedResource.getVault().getWorkspace().getId());
        cacheInvalidationService.invalidateVault(savedResource.getVault().getId());
        cacheInvalidationService.invalidateResource(savedResource.getId());
        
        return ApiResponse.success("Resource created", resourceMapper.toPublicResponse(savedResource));
    }
}
