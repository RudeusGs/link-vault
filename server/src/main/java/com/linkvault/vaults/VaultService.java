package com.linkvault.vaults;

import com.linkvault.common.exception.NotFoundException;
import com.linkvault.folders.Folder;
import com.linkvault.folders.FolderRepository;
import com.linkvault.resources.Resource;
import com.linkvault.resources.ResourceRepository;
import com.linkvault.resources.ResourceTagRepository;
import com.linkvault.resources.ResourceViewRepository;
import com.linkvault.users.User;
import com.linkvault.users.UserContextService;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VaultService {

    private final VaultRepository vaultRepository;
    private final FolderRepository folderRepository;
    private final ResourceRepository resourceRepository;
    private final ResourceTagRepository resourceTagRepository;
    private final ResourceViewRepository resourceViewRepository;
    private final UserContextService userContextService;

    public VaultService(
        VaultRepository vaultRepository,
        FolderRepository folderRepository,
        ResourceRepository resourceRepository,
        ResourceTagRepository resourceTagRepository,
        ResourceViewRepository resourceViewRepository,
        UserContextService userContextService
    ) {
        this.vaultRepository = vaultRepository;
        this.folderRepository = folderRepository;
        this.resourceRepository = resourceRepository;
        this.resourceTagRepository = resourceTagRepository;
        this.resourceViewRepository = resourceViewRepository;
        this.userContextService = userContextService;
    }

    @Transactional(readOnly = true)
    public List<VaultResponse> listVaults() {
        UUID userId = userContextService.getDemoUser().getId();
        return vaultRepository.findByUser_IdOrderByCreatedAtDesc(userId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public VaultResponse getVaultResponse(UUID id) {
        return toResponse(getVault(id));
    }

    @Transactional
    public VaultResponse create(VaultRequest request) {
        User user = userContextService.getDemoUser();

        Vault vault = new Vault();
        vault.setUser(user);
        applyRequest(vault, request);

        return toResponse(vaultRepository.save(vault));
    }

    @Transactional
    public VaultResponse update(UUID id, VaultRequest request) {
        Vault vault = getVault(id);
        applyRequest(vault, request);
        return toResponse(vaultRepository.save(vault));
    }

    @Transactional
    public void delete(UUID id) {
        Vault vault = getVault(id);

        for (Resource resource : resourceRepository.findByVault_IdOrderByCreatedAtDesc(id)) {
            resourceTagRepository.deleteByResource_Id(resource.getId());
            resourceViewRepository.deleteByResource_Id(resource.getId());
        }
        resourceRepository.deleteAll(resourceRepository.findByVault_IdOrderByCreatedAtDesc(id));

        List<Folder> folders = folderRepository.findByVault_IdOrderBySortOrderAscNameAsc(id);
        folders.forEach(folder -> folder.setParent(null));
        folderRepository.saveAll(folders);
        folderRepository.flush();
        folderRepository.deleteAll(folders);

        vaultRepository.delete(vault);
    }

    @Transactional(readOnly = true)
    public Vault getVault(UUID id) {
        return vaultRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Vault not found"));
    }

    public VaultResponse toResponse(Vault vault) {
        return new VaultResponse(
            vault.getId(),
            vault.getName(),
            vault.getDescription(),
            vault.getIcon(),
            vault.getColor(),
            vault.getCreatedAt(),
            vault.getUpdatedAt()
        );
    }

    private void applyRequest(Vault vault, VaultRequest request) {
        vault.setName(request.name().trim());
        vault.setDescription(request.description());
        vault.setIcon(request.icon());
        vault.setColor(request.color());
    }
}
