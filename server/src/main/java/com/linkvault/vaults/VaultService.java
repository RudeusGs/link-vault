package com.linkvault.vaults;

import com.linkvault.common.exception.BadRequestException;
import com.linkvault.common.exception.ErrorCode;
import com.linkvault.common.exception.ForbiddenException;
import com.linkvault.common.exception.NotFoundException;
import com.linkvault.folders.Folder;
import com.linkvault.folders.FolderRepository;
import com.linkvault.resources.Resource;
import com.linkvault.resources.ResourceCleanupService;
import com.linkvault.resources.ResourceRepository;
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
    private final ResourceCleanupService resourceCleanupService;
    private final UserContextService userContextService;

    public VaultService(
        VaultRepository vaultRepository,
        FolderRepository folderRepository,
        ResourceRepository resourceRepository,
        ResourceCleanupService resourceCleanupService,
        UserContextService userContextService
    ) {
        this.vaultRepository = vaultRepository;
        this.folderRepository = folderRepository;
        this.resourceRepository = resourceRepository;
        this.resourceCleanupService = resourceCleanupService;
        this.userContextService = userContextService;
    }

    @Transactional(readOnly = true)
    public List<VaultResponse> listVaults() {
        UUID userId = userContextService.getCurrentUser().getId();
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
        User user = userContextService.getCurrentUser();

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

        List<Resource> resources = resourceRepository.findByVault_IdOrderByCreatedAtDesc(id);
        resourceCleanupService.deleteAll(resources);

        List<Folder> folders = folderRepository.findByVault_IdOrderBySortOrderAscNameAsc(id);
        folders.forEach(folder -> folder.setParent(null));
        folderRepository.saveAll(folders);
        folderRepository.flush();
        folderRepository.deleteAll(folders);

        vaultRepository.delete(vault);
    }

    @Transactional(readOnly = true)
    public Vault getVault(UUID id) {
        Vault vault = vaultRepository.findById(id)
            .orElseThrow(() -> new NotFoundException(ErrorCode.VAULT_NOT_FOUND, "Vault not found"));
        ensureOwner(vault);
        return vault;
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

    private void ensureOwner(Vault vault) {
        UUID currentUserId = userContextService.getCurrentUser().getId();
        if (!vault.getUser().getId().equals(currentUserId)) {
            throw new ForbiddenException(ErrorCode.VAULT_ACCESS_DENIED, "You do not have access to this vault");
        }
    }

    private void applyRequest(Vault vault, VaultRequest request) {
        String name = normalizeName(request.name());
        ensureNameAvailable(vault.getUser().getId(), name, vault.getId());

        vault.setName(name);
        vault.setDescription(trimToNull(request.description()));
        vault.setIcon(trimToNull(request.icon()));
        vault.setColor(trimToNull(request.color()));
    }

    private void ensureNameAvailable(UUID userId, String name, UUID excludedVaultId) {
        boolean exists = excludedVaultId == null
            ? vaultRepository.existsByUser_IdAndNameIgnoreCase(userId, name)
            : vaultRepository.existsByUser_IdAndNameIgnoreCaseAndIdNot(userId, name, excludedVaultId);

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
}
