package com.linkvault.folders;

import com.linkvault.common.exception.BadRequestException;
import com.linkvault.common.exception.NotFoundException;
import com.linkvault.resources.Resource;
import com.linkvault.resources.ResourceRepository;
import com.linkvault.resources.ResourceTagRepository;
import com.linkvault.resources.ResourceViewRepository;
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
    private final ResourceTagRepository resourceTagRepository;
    private final ResourceViewRepository resourceViewRepository;
    private final VaultService vaultService;

    public FolderService(
        FolderRepository folderRepository,
        ResourceRepository resourceRepository,
        ResourceTagRepository resourceTagRepository,
        ResourceViewRepository resourceViewRepository,
        VaultService vaultService
    ) {
        this.folderRepository = folderRepository;
        this.resourceRepository = resourceRepository;
        this.resourceTagRepository = resourceTagRepository;
        this.resourceViewRepository = resourceViewRepository;
        this.vaultService = vaultService;
    }

    @Transactional(readOnly = true)
    public List<FolderResponse> listByVault(UUID vaultId) {
        return folderRepository.findByVault_IdOrderBySortOrderAscNameAsc(vaultId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<FolderResponse> listChildren(UUID parentId) {
        return folderRepository.findByParent_IdOrderBySortOrderAscNameAsc(parentId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public FolderResponse getFolderResponse(UUID id) {
        return toResponse(getFolder(id));
    }

    @Transactional
    public FolderResponse create(FolderRequest request) {
        Folder folder = new Folder();
        applyRequest(folder, request);
        return toResponse(folderRepository.save(folder));
    }

    @Transactional
    public FolderResponse update(UUID id, FolderRequest request) {
        Folder folder = getFolder(id);
        applyRequest(folder, request);
        return toResponse(folderRepository.save(folder));
    }

    @Transactional
    public void delete(UUID id) {
        Folder folder = getFolder(id);
        deleteFolderTree(folder);
    }

    @Transactional(readOnly = true)
    public Folder getFolder(UUID id) {
        return folderRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Folder not found"));
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

    private void applyRequest(Folder folder, FolderRequest request) {
        Vault vault = vaultService.getVault(request.vaultId());
        Folder parent = request.parentId() == null ? null : getFolder(request.parentId());

        if (parent != null && !parent.getVault().getId().equals(vault.getId())) {
            throw new BadRequestException("Parent folder must belong to the same vault");
        }

        folder.setVault(vault);
        folder.setParent(parent);
        folder.setName(request.name().trim());
        folder.setDescription(request.description());
        folder.setIcon(request.icon());
        folder.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
    }

    private void deleteFolderTree(Folder folder) {
        for (Folder child : folderRepository.findByParent_IdOrderBySortOrderAscNameAsc(folder.getId())) {
            deleteFolderTree(child);
        }

        for (Resource resource : resourceRepository.findByFolder_IdOrderByCreatedAtDesc(folder.getId())) {
            resourceTagRepository.deleteByResource_Id(resource.getId());
            resourceViewRepository.deleteByResource_Id(resource.getId());
        }
        resourceRepository.deleteAll(resourceRepository.findByFolder_IdOrderByCreatedAtDesc(folder.getId()));
        folderRepository.delete(folder);
    }
}
