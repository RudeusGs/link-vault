package com.linkvault.dashboard;

import com.linkvault.folders.FolderRepository;
import com.linkvault.resources.ResourceRepository;
import com.linkvault.resources.ResourceService;
import com.linkvault.resources.ResourceType;
import com.linkvault.tags.TagRepository;
import com.linkvault.tags.TagService;
import com.linkvault.users.User;
import com.linkvault.users.UserContextService;
import com.linkvault.vaults.VaultRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private final VaultRepository vaultRepository;
    private final FolderRepository folderRepository;
    private final ResourceRepository resourceRepository;
    private final ResourceService resourceService;
    private final TagRepository tagRepository;
    private final TagService tagService;
    private final UserContextService userContextService;

    public DashboardService(
        VaultRepository vaultRepository,
        FolderRepository folderRepository,
        ResourceRepository resourceRepository,
        ResourceService resourceService,
        TagRepository tagRepository,
        TagService tagService,
        UserContextService userContextService
    ) {
        this.vaultRepository = vaultRepository;
        this.folderRepository = folderRepository;
        this.resourceRepository = resourceRepository;
        this.resourceService = resourceService;
        this.tagRepository = tagRepository;
        this.tagService = tagService;
        this.userContextService = userContextService;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse summary() {
        User user = userContextService.getDemoUser();

        return new DashboardSummaryResponse(
            vaultRepository.countByUser_Id(user.getId()),
            folderRepository.countByVault_User_Id(user.getId()),
            resourceRepository.countByVault_User_Id(user.getId()),
            resourceRepository.countByVault_User_IdAndResourceType(user.getId(), ResourceType.LINK),
            resourceRepository.countByVault_User_IdAndResourceType(user.getId(), ResourceType.FILE),
            resourceRepository.countByVault_User_IdAndResourceType(user.getId(), ResourceType.NOTE),
            resourceRepository.countByVault_User_IdAndResourceType(user.getId(), ResourceType.SNIPPET),
            resourceRepository.countByVault_User_IdAndIsFavoriteTrue(user.getId()),
            resourceRepository.findTop6ByVault_User_IdOrderByCreatedAtDesc(user.getId()).stream()
                .map(resourceService::toResponse)
                .toList(),
            tagService.topTags(tagRepository.findByUser_IdOrderByNameAsc(user.getId()), 8)
        );
    }
}
