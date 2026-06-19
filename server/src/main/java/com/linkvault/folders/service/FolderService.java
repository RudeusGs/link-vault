package com.linkvault.folders.service;

import com.linkvault.audit.service.AuditLogService;
import com.linkvault.common.exception.BadRequestException;
import com.linkvault.common.exception.ErrorCode;
import com.linkvault.common.exception.NotFoundException;
import com.linkvault.common.redis.RedisCacheInvalidationService;
import com.linkvault.common.redis.RedisCacheService;
import com.linkvault.common.redis.RedisKeys;
import com.linkvault.common.redis.RedisProperties;
import com.linkvault.folders.dto.FolderRequest;
import com.linkvault.folders.dto.FolderResponse;
import com.linkvault.folders.entity.Folder;
import com.linkvault.folders.repository.FolderRepository;
import com.linkvault.resources.entity.Resource;
import com.linkvault.resources.repository.ResourceRepository;
import com.linkvault.resources.service.ResourceCleanupService;
import com.linkvault.users.entity.User;
import com.linkvault.users.service.UserContextService;
import com.linkvault.vaults.entity.Vault;
import com.linkvault.vaults.service.VaultService;
import com.linkvault.workspaces.service.WorkspaceService;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FolderService {

    private final FolderRepository folderRepository;
    private final ResourceRepository resourceRepository;
    private final ResourceCleanupService resourceCleanupService;
    private final VaultService vaultService;
    private final WorkspaceService workspaceService;
    private final UserContextService userContextService;
    private final AuditLogService auditLogService;
    private final RedisCacheService redisCacheService;
    private final RedisProperties redisProperties;
    private final RedisCacheInvalidationService cacheInvalidationService;

    public FolderService(
        FolderRepository folderRepository,
        ResourceRepository resourceRepository,
        ResourceCleanupService resourceCleanupService,
        VaultService vaultService,
        WorkspaceService workspaceService,
        UserContextService userContextService,
        AuditLogService auditLogService,
        RedisCacheService redisCacheService,
        RedisProperties redisProperties,
        RedisCacheInvalidationService cacheInvalidationService
    ) {
        this.folderRepository = folderRepository;
        this.resourceRepository = resourceRepository;
        this.resourceCleanupService = resourceCleanupService;
        this.vaultService = vaultService;
        this.workspaceService = workspaceService;
        this.userContextService = userContextService;
        this.auditLogService = auditLogService;
        this.redisCacheService = redisCacheService;
        this.redisProperties = redisProperties;
        this.cacheInvalidationService = cacheInvalidationService;
    }

    @Transactional(readOnly = true)
    public List<FolderResponse> listByVault(UUID vaultId) {
        vaultService.getVault(vaultId);
        return cachedFoldersByVault(vaultId);
    }

    @Transactional(readOnly = true)
    public List<FolderResponse> listByVault(UUID workspaceId, UUID vaultId) {
        vaultService.getVault(workspaceId, vaultId);
        return cachedFoldersByVault(vaultId);
    }

    @Transactional(readOnly = true)
    public List<FolderResponse> listChildren(UUID parentId) {
        Folder parent = getFolder(parentId);
        return cachedChildren(parent.getId());
    }

    @Transactional(readOnly = true)
    public List<FolderResponse> listChildren(UUID workspaceId, UUID parentId) {
        Folder parent = getFolder(workspaceId, parentId);
        return cachedChildren(parent.getId());
    }

    @Transactional(readOnly = true)
    public FolderResponse getFolderResponse(UUID id) {
        return toResponse(getFolder(id));
    }

    @Transactional(readOnly = true)
    public FolderResponse getFolderResponse(UUID workspaceId, UUID id) {
        return toResponse(getFolder(workspaceId, id));
    }

    @Transactional
    public FolderResponse createInVault(UUID vaultId, FolderRequest request) {
        Vault vault = vaultService.getVaultForWrite(vaultId);

        Folder folder = new Folder();
        applyRequest(folder, request, vault, null);
        Folder savedFolder = folderRepository.save(folder);
        auditLogService.recordAsync(vault.getWorkspace(), userContextService.getCurrentUser(), "folder.created", "FOLDER", savedFolder.getId());
        invalidateFolderCaches(savedFolder);
        return toResponse(savedFolder);
    }

    @Transactional
    public FolderResponse createInVault(UUID workspaceId, UUID vaultId, FolderRequest request) {
        Vault vault = vaultService.getVaultForWrite(workspaceId, vaultId);

        Folder folder = new Folder();
        applyRequest(folder, request, vault, null);
        Folder savedFolder = folderRepository.save(folder);
        auditLogService.recordAsync(vault.getWorkspace(), userContextService.getCurrentUser(), "folder.created", "FOLDER", savedFolder.getId());
        invalidateFolderCaches(savedFolder);
        return toResponse(savedFolder);
    }

    @Transactional
    public FolderResponse createChild(UUID parentId, FolderRequest request) {
        Folder parent = getFolderForWrite(parentId);

        Folder folder = new Folder();
        applyRequest(folder, request, parent.getVault(), parent);
        Folder savedFolder = folderRepository.save(folder);
        auditLogService.recordAsync(parent.getVault().getWorkspace(), userContextService.getCurrentUser(), "folder.created", "FOLDER", savedFolder.getId());
        invalidateFolderCaches(savedFolder);
        return toResponse(savedFolder);
    }

    @Transactional
    public FolderResponse createChild(UUID workspaceId, UUID parentId, FolderRequest request) {
        Folder parent = getFolderForWrite(workspaceId, parentId);

        Folder folder = new Folder();
        applyRequest(folder, request, parent.getVault(), parent);
        Folder savedFolder = folderRepository.save(folder);
        auditLogService.recordAsync(parent.getVault().getWorkspace(), userContextService.getCurrentUser(), "folder.created", "FOLDER", savedFolder.getId());
        invalidateFolderCaches(savedFolder);
        return toResponse(savedFolder);
    }

    @Transactional
    public FolderResponse update(UUID id, FolderRequest request) {
        Folder folder = getFolderForWrite(id);

        applyRequest(folder, request, folder.getVault(), folder.getParent());
        Folder savedFolder = folderRepository.save(folder);
        auditLogService.recordAsync(savedFolder.getVault().getWorkspace(), userContextService.getCurrentUser(), "folder.updated", "FOLDER", savedFolder.getId());
        invalidateFolderCaches(savedFolder);
        return toResponse(savedFolder);
    }

    @Transactional
    public FolderResponse update(UUID workspaceId, UUID id, FolderRequest request) {
        Folder folder = getFolderForWrite(workspaceId, id);

        applyRequest(folder, request, folder.getVault(), folder.getParent());
        Folder savedFolder = folderRepository.save(folder);
        auditLogService.recordAsync(savedFolder.getVault().getWorkspace(), userContextService.getCurrentUser(), "folder.updated", "FOLDER", savedFolder.getId());
        invalidateFolderCaches(savedFolder);
        return toResponse(savedFolder);
    }

    @Transactional
    public void delete(UUID id) {
        Folder folder = getFolderForWrite(id);
        User actor = userContextService.getCurrentUser();
        auditLogService.recordAsync(folder.getVault().getWorkspace(), actor, "folder.deleted", "FOLDER", folder.getId());
        deleteFolderTree(folder);
    }

    @Transactional
    public void delete(UUID workspaceId, UUID id) {
        Folder folder = getFolderForWrite(workspaceId, id);
        User actor = userContextService.getCurrentUser();
        auditLogService.recordAsync(folder.getVault().getWorkspace(), actor, "folder.deleted", "FOLDER", folder.getId());
        deleteFolderTree(folder);
    }

    @Transactional(readOnly = true)
    public Folder getFolder(UUID id) {
        Folder folder = folderRepository.findById(id)
            .orElseThrow(() -> new NotFoundException(ErrorCode.FOLDER_NOT_FOUND, "Folder not found"));
        workspaceService.requireMember(folder.getVault().getWorkspace().getId());
        return folder;
    }

    @Transactional(readOnly = true)
    public Folder getFolder(UUID workspaceId, UUID id) {
        workspaceService.requireMember(workspaceId);
        return folderRepository.findByIdAndVault_Workspace_Id(id, workspaceId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.FOLDER_NOT_FOUND, "Folder not found"));
    }

    @Transactional(readOnly = true)
    public Folder getFolderForWrite(UUID id) {
        Folder folder = folderRepository.findById(id)
            .orElseThrow(() -> new NotFoundException(ErrorCode.FOLDER_NOT_FOUND, "Folder not found"));
        workspaceService.requireCanWrite(folder.getVault().getWorkspace().getId());
        return folder;
    }

    @Transactional(readOnly = true)
    public Folder getFolderForWrite(UUID workspaceId, UUID id) {
        workspaceService.requireCanWrite(workspaceId);
        return folderRepository.findByIdAndVault_Workspace_Id(id, workspaceId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.FOLDER_NOT_FOUND, "Folder not found"));
    }

    private List<FolderResponse> cachedFoldersByVault(UUID vaultId) {
        return redisCacheService.getListOrLoad(
            RedisKeys.vaultFolders(vaultId),
            redisProperties.getCache().getFolderTreeTtl(),
            FolderResponse.class,
            () -> folderRepository.findByVault_IdOrderBySortOrderAscNameAsc(vaultId).stream()
                .map(this::toResponse)
                .toList()
        );
    }

    private List<FolderResponse> cachedChildren(UUID parentId) {
        return redisCacheService.getListOrLoad(
            RedisKeys.folderChildren(parentId),
            redisProperties.getCache().getFolderTreeTtl(),
            FolderResponse.class,
            () -> folderRepository.findByParent_IdOrderBySortOrderAscNameAsc(parentId).stream()
                .map(this::toResponse)
                .toList()
        );
    }

    public FolderResponse toResponse(Folder folder) {
        return new FolderResponse(
            folder.getId(),
            folder.getVault().getId(),
            folder.getParent() == null ? null : folder.getParent().getId(),
            folder.getName(),
            folder.getDescription(),
            folder.getIcon(),
            folder.getSortOrder(),
            folder.getCreatedAt(),
            folder.getUpdatedAt()
        );
    }

    private void applyRequest(Folder folder, FolderRequest request, Vault vault, Folder parent) {
        String name = normalizeName(request.name());
        ensureNameAvailable(vault.getId(), parent == null ? null : parent.getId(), name, folder.getId());

        folder.setVault(vault);
        folder.setParent(parent);
        folder.setName(name);
        folder.setDescription(trimToNull(request.description()));
        folder.setIcon(trimToNull(request.icon()));
        folder.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
    }

    private void ensureNameAvailable(UUID vaultId, UUID parentId, String name, UUID excludedFolderId) {
        boolean exists;
        if (parentId == null) {
            exists = excludedFolderId == null
                ? folderRepository.existsByVault_IdAndParentIsNullAndNameIgnoreCase(vaultId, name)
                : folderRepository.existsByVault_IdAndParentIsNullAndNameIgnoreCaseAndIdNot(vaultId, name, excludedFolderId);
        } else {
            exists = excludedFolderId == null
                ? folderRepository.existsByVault_IdAndParent_IdAndNameIgnoreCase(vaultId, parentId, name)
                : folderRepository.existsByVault_IdAndParent_IdAndNameIgnoreCaseAndIdNot(vaultId, parentId, name, excludedFolderId);
        }

        if (exists) {
            throw new BadRequestException("Folder name already exists in this location");
        }
    }

    private void deleteFolderTree(Folder folder) {
        for (Folder child : folderRepository.findByParent_IdOrderBySortOrderAscNameAsc(folder.getId())) {
            deleteFolderTree(child);
        }

        List<Resource> resources = resourceRepository.findByFolder_IdOrderByCreatedAtDesc(folder.getId());
        resourceCleanupService.deleteAll(resources);
        folderRepository.delete(folder);
        resources.forEach(resource -> cacheInvalidationService.invalidateResource(resource.getId()));
        invalidateFolderCaches(folder);
    }

    private void invalidateFolderCaches(Folder folder) {
        cacheInvalidationService.invalidateWorkspace(folder.getVault().getWorkspace().getId());
        cacheInvalidationService.invalidateVault(folder.getVault().getId());
        cacheInvalidationService.invalidateFolder(folder.getId());
        if (folder.getParent() != null) {
            cacheInvalidationService.invalidateFolder(folder.getParent().getId());
        }
    }

    private String normalizeName(String value) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException("Folder name is required");
        }
        return value.trim();
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}

