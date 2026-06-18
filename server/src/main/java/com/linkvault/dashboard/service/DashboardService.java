package com.linkvault.dashboard.service;

import com.linkvault.dashboard.dto.DashboardSummaryResponse;
import com.linkvault.common.redis.RedisCacheService;
import com.linkvault.common.redis.RedisKeys;
import com.linkvault.common.redis.RedisProperties;
import com.linkvault.folders.repository.FolderRepository;
import com.linkvault.resources.enums.ResourceType;
import com.linkvault.resources.repository.ResourceRepository;
import com.linkvault.resources.service.ResourceService;
import com.linkvault.tags.repository.TagRepository;
import com.linkvault.tags.service.TagService;
import com.linkvault.vaults.repository.VaultRepository;
import com.linkvault.workspaces.entity.Workspace;
import com.linkvault.workspaces.service.WorkspaceService;
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
    private final WorkspaceService workspaceService;
    private final RedisCacheService redisCacheService;
    private final RedisProperties redisProperties;

    public DashboardService(
        VaultRepository vaultRepository,
        FolderRepository folderRepository,
        ResourceRepository resourceRepository,
        ResourceService resourceService,
        TagRepository tagRepository,
        TagService tagService,
        WorkspaceService workspaceService,
        RedisCacheService redisCacheService,
        RedisProperties redisProperties
    ) {
        this.vaultRepository = vaultRepository;
        this.folderRepository = folderRepository;
        this.resourceRepository = resourceRepository;
        this.resourceService = resourceService;
        this.tagRepository = tagRepository;
        this.tagService = tagService;
        this.workspaceService = workspaceService;
        this.redisCacheService = redisCacheService;
        this.redisProperties = redisProperties;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse summary() {
        Workspace workspace = workspaceService.getDefaultWorkspaceForCurrentUser();
        return summary(workspace.getId());
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse summary(java.util.UUID workspaceId) {
        workspaceService.requireMember(workspaceId);
        return redisCacheService.getOrLoad(
            RedisKeys.workspaceDashboard(workspaceId),
            redisProperties.getCache().getDashboardSummaryTtl(),
            DashboardSummaryResponse.class,
            () -> new DashboardSummaryResponse(
                vaultRepository.countByWorkspace_Id(workspaceId),
                folderRepository.countByVault_Workspace_Id(workspaceId),
                resourceRepository.countByVault_Workspace_Id(workspaceId),
                resourceRepository.countByVault_Workspace_IdAndResourceType(workspaceId, ResourceType.LINK),
                resourceRepository.countByVault_Workspace_IdAndResourceType(workspaceId, ResourceType.FILE),
                resourceRepository.countByVault_Workspace_IdAndResourceType(workspaceId, ResourceType.NOTE),
                resourceRepository.countByVault_Workspace_IdAndResourceType(workspaceId, ResourceType.SNIPPET),
                resourceRepository.countByVault_Workspace_IdAndIsFavoriteTrue(workspaceId),
                resourceService.toResponses(resourceRepository.findTop6ByVault_Workspace_IdOrderByCreatedAtDesc(workspaceId)),
                tagService.topTags(tagRepository.findByWorkspace_IdOrderByNameAsc(workspaceId), 8)
            )
        );
    }
}

