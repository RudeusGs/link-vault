package com.linkvault.folders;

import com.linkvault.common.exception.BadRequestException;
import com.linkvault.common.exception.ErrorCode;
import com.linkvault.common.exception.ForbiddenException;
import com.linkvault.common.exception.NotFoundException;
import com.linkvault.resources.Resource;
import com.linkvault.resources.ResourceCleanupService;
import com.linkvault.resources.ResourceRepository;
import com.linkvault.users.UserContextService;
import com.linkvault.vaults.Vault;
import com.linkvault.vaults.VaultService;
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
    private final UserContextService userContextService;

    public FolderService(
        FolderRepository folderRepository,
        ResourceRepository resourceRepository,
        ResourceCleanupService resourceCleanupService,
        VaultService vaultService,
        UserContextService userContextService
    ) {
        this.folderRepository = folderRepository;
        this.resourceRepository = resourceRepository;
        this.resourceCleanupService = resourceCleanupService;
        this.vaultService = vaultService;
        this.userContextService = userContextService;
    }

    @Transactional(readOnly = true)
    public List<FolderResponse> listByVault(UUID vaultId) {
        vaultService.getVault(vaultId);
        return folderRepository.findByVault_IdOrderBySortOrderAscNameAsc(vaultId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<FolderResponse> listChildren(UUID parentId) {
        Folder parent = getFolder(parentId);
        return folderRepository.findByParent_IdOrderBySortOrderAscNameAsc(parent.getId()).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public FolderResponse getFolderResponse(UUID id) {
        return toResponse(getFolder(id));
    }

    @Transactional
    public FolderResponse createInVault(UUID vaultId, FolderRequest request) {
        Vault vault = vaultService.getVault(vaultId);

        Folder folder = new Folder();
        applyRequest(folder, request, vault, null);
        return toResponse(folderRepository.save(folder));
    }

    @Transactional
    public FolderResponse createChild(UUID parentId, FolderRequest request) {
        Folder parent = getFolder(parentId);

        Folder folder = new Folder();
        applyRequest(folder, request, parent.getVault(), parent);
        return toResponse(folderRepository.save(folder));
    }

    @Transactional
    public FolderResponse update(UUID id, FolderRequest request) {
        Folder folder = getFolder(id);

        applyRequest(folder, request, folder.getVault(), folder.getParent());
        return toResponse(folderRepository.save(folder));
    }

    @Transactional
    public void delete(UUID id) {
        Folder folder = getFolder(id);
        deleteFolderTree(folder);
    }

    @Transactional(readOnly = true)
    public Folder getFolder(UUID id) {
        Folder folder = folderRepository.findById(id)
            .orElseThrow(() -> new NotFoundException(ErrorCode.FOLDER_NOT_FOUND, "Folder not found"));
        ensureOwner(folder);
        return folder;
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

    private void ensureOwner(Folder folder) {
        UUID currentUserId = userContextService.getCurrentUser().getId();
        if (!folder.getVault().getUser().getId().equals(currentUserId)) {
            throw new ForbiddenException(ErrorCode.FOLDER_ACCESS_DENIED, "You do not have access to this folder");
        }
    }

    private void deleteFolderTree(Folder folder) {
        for (Folder child : folderRepository.findByParent_IdOrderBySortOrderAscNameAsc(folder.getId())) {
            deleteFolderTree(child);
        }

        List<Resource> resources = resourceRepository.findByFolder_IdOrderByCreatedAtDesc(folder.getId());
        resourceCleanupService.deleteAll(resources);
        folderRepository.delete(folder);
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
