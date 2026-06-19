package com.linkvault.vaults.service;

import com.linkvault.audit.service.AuditLogService;
import com.linkvault.common.exception.BadRequestException;
import com.linkvault.common.exception.ErrorCode;
import com.linkvault.common.exception.NotFoundException;
import com.linkvault.common.redis.RedisCacheInvalidationService;
import com.linkvault.common.redis.RedisCacheService;
import com.linkvault.common.redis.RedisKeys;
import com.linkvault.common.redis.RedisProperties;
import com.linkvault.folders.entity.Folder;
import com.linkvault.folders.repository.FolderRepository;
import com.linkvault.resources.entity.Resource;
import com.linkvault.resources.repository.ResourceRepository;
import com.linkvault.resources.service.ResourceCleanupService;
import com.linkvault.users.entity.User;
import com.linkvault.users.service.UserContextService;
import com.linkvault.vaults.dto.VaultRequest;
import com.linkvault.vaults.dto.VaultResponse;
import com.linkvault.vaults.entity.Vault;
import com.linkvault.vaults.repository.VaultRepository;
import com.linkvault.workspaces.entity.Workspace;
import com.linkvault.workspaces.service.QuotaService;
import com.linkvault.workspaces.service.WorkspaceService;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VaultService {

    private final VaultRepository vaultRepository;
    private final FolderRepository folderRepository;
    private final ResourceRepository resourceRepository;
    private final ResourceCleanupService resourceCleanupService;
    private final UserContextService userContextService;
    private final WorkspaceService workspaceService;
    private final QuotaService quotaService;
    private final AuditLogService auditLogService;
    private final RedisCacheService redisCacheService;
    private final RedisProperties redisProperties;
    private final RedisCacheInvalidationService cacheInvalidationService;

    public VaultService(
        VaultRepository vaultRepository,
        FolderRepository folderRepository,
        ResourceRepository resourceRepository,
        ResourceCleanupService resourceCleanupService,
        UserContextService userContextService,
        WorkspaceService workspaceService,
        QuotaService quotaService,
        AuditLogService auditLogService,
        RedisCacheService redisCacheService,
        RedisProperties redisProperties,
        RedisCacheInvalidationService cacheInvalidationService
    ) {
        this.vaultRepository = vaultRepository;
        this.folderRepository = folderRepository;
        this.resourceRepository = resourceRepository;
        this.resourceCleanupService = resourceCleanupService;
        this.userContextService = userContextService;
        this.workspaceService = workspaceService;
        this.quotaService = quotaService;
        this.auditLogService = auditLogService;
        this.redisCacheService = redisCacheService;
        this.redisProperties = redisProperties;
        this.cacheInvalidationService = cacheInvalidationService;
    }

    @Transactional(readOnly = true)
    public List<VaultResponse> listVaults() {
        Workspace workspace = workspaceService.getDefaultWorkspaceForCurrentUser();
        return listVaults(workspace.getId());
    }

    @Transactional(readOnly = true)
    public List<VaultResponse> listVaults(UUID workspaceId) {
        workspaceService.requireMember(workspaceId);
        return redisCacheService.getListOrLoad(
            RedisKeys.workspaceVaultList(workspaceId),
            redisProperties.getCache().getVaultListTtl(),
            VaultResponse.class,
            () -> vaultRepository.findByWorkspace_IdOrderByCreatedAtDesc(workspaceId).stream()
                .map(this::toResponse)
                .toList()
        );
    }

    @Transactional(readOnly = true)
    public VaultResponse getVaultResponse(UUID id) {
        return toResponse(getVault(id));
    }

    @Transactional(readOnly = true)
    public VaultResponse getVaultResponse(UUID workspaceId, UUID id) {
        return toResponse(getVault(workspaceId, id));
    }

    @Transactional
    public VaultResponse create(VaultRequest request) {
        Workspace workspace = workspaceService.getDefaultWorkspaceForCurrentUser();
        return create(workspace.getId(), request);
    }

    @Transactional
    public VaultResponse create(UUID workspaceId, VaultRequest request) {
        User user = userContextService.getCurrentUser();
        Workspace workspace = workspaceService.requireCanWrite(workspaceId);
        quotaService.requireCanCreateVault(workspace);

        Vault vault = new Vault();
        vault.setUser(user);
        vault.setWorkspace(workspace);
        applyRequest(vault, request);

        Vault savedVault = vaultRepository.save(vault);
        auditLogService.recordAsync(workspace, user, "vault.created", "VAULT", savedVault.getId());
        invalidateVaultCaches(savedVault);
        return toResponse(savedVault);
    }

    @Transactional
    public VaultResponse update(UUID id, VaultRequest request) {
        Vault vault = getVaultForWrite(id);
        applyRequest(vault, request);
        Vault savedVault = vaultRepository.save(vault);
        auditLogService.recordAsync(
            savedVault.getWorkspace(),
            userContextService.getCurrentUser(),
            "vault.updated",
            "VAULT",
            savedVault.getId()
        );
        invalidateVaultCaches(savedVault);
        return toResponse(savedVault);
    }

    @Transactional
    public VaultResponse update(UUID workspaceId, UUID id, VaultRequest request) {
        Vault vault = getVaultForWrite(workspaceId, id);
        applyRequest(vault, request);
        Vault savedVault = vaultRepository.save(vault);
        auditLogService.recordAsync(
            savedVault.getWorkspace(),
            userContextService.getCurrentUser(),
            "vault.updated",
            "VAULT",
            savedVault.getId()
        );
        invalidateVaultCaches(savedVault);
        return toResponse(savedVault);
    }

    @Transactional
    public void delete(UUID id) {
        Vault vault = getVaultForWrite(id);
        deleteVault(vault);
    }

    @Transactional
    public void delete(UUID workspaceId, UUID id) {
        Vault vault = getVaultForWrite(workspaceId, id);
        deleteVault(vault);
    }

    private void deleteVault(Vault vault) {
        UUID id = vault.getId();
        Workspace workspace = vault.getWorkspace();
        User user = userContextService.getCurrentUser();
        List<Resource> resources = resourceRepository.findByVault_IdOrderByCreatedAtDesc(id);
        resourceCleanupService.deleteAll(resources);

        List<Folder> folders = folderRepository.findByVault_IdOrderBySortOrderAscNameAsc(id);
        folders.forEach(folder -> folder.setParent(null));
        folderRepository.saveAll(folders);
        folderRepository.flush();
        folderRepository.deleteAll(folders);

        vaultRepository.delete(vault);
        auditLogService.recordAsync(workspace, user, "vault.deleted", "VAULT", id);
        cacheInvalidationService.invalidateWorkspace(workspace.getId());
        cacheInvalidationService.invalidateVault(id);
        resources.forEach(resource -> cacheInvalidationService.invalidateResource(resource.getId()));
        folders.forEach(folder -> cacheInvalidationService.invalidateFolder(folder.getId()));
    }

    @Transactional(readOnly = true)
    public Vault getVault(UUID id) {
        Vault vault = vaultRepository.findById(id)
            .orElseThrow(() -> new NotFoundException(ErrorCode.VAULT_NOT_FOUND, "Vault not found"));
        workspaceService.requireMember(vault.getWorkspace().getId());
        return vault;
    }

    @Transactional(readOnly = true)
    public Vault getVault(UUID workspaceId, UUID id) {
        workspaceService.requireMember(workspaceId);
        return vaultRepository.findByIdAndWorkspace_Id(id, workspaceId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.VAULT_NOT_FOUND, "Vault not found"));
    }

    @Transactional(readOnly = true)
    public Vault getVaultForWrite(UUID id) {
        Vault vault = vaultRepository.findById(id)
            .orElseThrow(() -> new NotFoundException(ErrorCode.VAULT_NOT_FOUND, "Vault not found"));
        workspaceService.requireCanWrite(vault.getWorkspace().getId());
        return vault;
    }

    @Transactional(readOnly = true)
    public Vault getVaultForWrite(UUID workspaceId, UUID id) {
        workspaceService.requireCanWrite(workspaceId);
        Vault vault = vaultRepository.findByIdAndWorkspace_Id(id, workspaceId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.VAULT_NOT_FOUND, "Vault not found"));
        return vault;
    }

    public VaultResponse toResponse(Vault vault) {
        return new VaultResponse(
            vault.getId(),
            vault.getName(),
            vault.getDescription(),
            vault.getIcon(),
            vault.getColor(),
            vault.getPublicAccess(),
            vault.getCreatedAt(),
            vault.getUpdatedAt()
        );
    }


    private void invalidateVaultCaches(Vault vault) {
        cacheInvalidationService.invalidateWorkspace(vault.getWorkspace().getId());
        cacheInvalidationService.invalidateVault(vault.getId());
    }

    private void applyRequest(Vault vault, VaultRequest request) {
        String name = normalizeName(request.name());
        ensureNameAvailable(vault.getWorkspace().getId(), name, vault.getId());

        vault.setName(name);
        vault.setDescription(trimToNull(request.description()));
        vault.setIcon(trimToNull(request.icon()));
        vault.setColor(trimToNull(request.color()));
        
        if (request.publicAccess() != null) {
            vault.setPublicAccess(request.publicAccess());
        }
    }

    private void ensureNameAvailable(UUID workspaceId, String name, UUID excludedVaultId) {
        boolean exists = excludedVaultId == null
            ? vaultRepository.existsByWorkspace_IdAndNameIgnoreCase(workspaceId, name)
            : vaultRepository.existsByWorkspace_IdAndNameIgnoreCaseAndIdNot(workspaceId, name, excludedVaultId);

        if (exists) {
            throw new BadRequestException("Vault name already exists");
        }
    }

    private String normalizeName(String value) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException("Vault name is required");
        }
        return value.trim();
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
    @Transactional(readOnly = true)
    public VaultResponse getPublicVault(UUID id) {
        Vault vault = vaultRepository.findById(id)
            .orElseThrow(() -> new NotFoundException(ErrorCode.VAULT_NOT_FOUND, "Vault not found"));
        
        if (vault.getPublicAccess() == com.linkvault.common.enums.PublicAccess.PRIVATE) {
            throw new com.linkvault.common.exception.UnauthorizedException("This vault is private");
        }
        
        return toResponse(vault);
    }
}

