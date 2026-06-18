package com.linkvault.workspaces.service;

import com.linkvault.common.exception.BadRequestException;
import com.linkvault.common.exception.ErrorCode;
import com.linkvault.common.redis.RedisCacheService;
import com.linkvault.common.redis.RedisKeys;
import com.linkvault.common.redis.RedisProperties;
import com.linkvault.resources.repository.ResourceRepository;
import com.linkvault.vaults.repository.VaultRepository;
import com.linkvault.workspaces.dto.WorkspaceUsageResponse;
import com.linkvault.workspaces.entity.Workspace;
import com.linkvault.workspaces.enums.WorkspacePlan;
import com.linkvault.workspaces.repository.WorkspaceMemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuotaService {

    private static final long MIB = 1024L * 1024L;

    private final VaultRepository vaultRepository;
    private final ResourceRepository resourceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final RedisCacheService redisCacheService;
    private final RedisProperties redisProperties;

    public QuotaService(
        VaultRepository vaultRepository,
        ResourceRepository resourceRepository,
        WorkspaceMemberRepository workspaceMemberRepository,
        RedisCacheService redisCacheService,
        RedisProperties redisProperties
    ) {
        this.vaultRepository = vaultRepository;
        this.resourceRepository = resourceRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.redisCacheService = redisCacheService;
        this.redisProperties = redisProperties;
    }

    @Transactional(readOnly = true)
    public WorkspaceUsageResponse usage(Workspace workspace) {
        return redisCacheService.getOrLoad(
            RedisKeys.workspaceUsage(workspace.getId()),
            redisProperties.getCache().getWorkspaceUsageTtl(),
            WorkspaceUsageResponse.class,
            () -> buildUsage(workspace)
        );
    }

    private WorkspaceUsageResponse buildUsage(Workspace workspace) {
        WorkspacePlanLimits limits = limitsFor(workspace.getPlan());
        long vaultCount = vaultRepository.countByWorkspace_Id(workspace.getId());
        long memberCount = workspaceMemberRepository.countByWorkspace_IdAndDeletedAtIsNull(workspace.getId());
        long resourceCount = resourceRepository.countByVault_Workspace_Id(workspace.getId());
        long storageUsedBytes = resourceRepository.sumFileSizeByWorkspaceId(workspace.getId());

        return new WorkspaceUsageResponse(
            workspace.getPlan(),
            vaultCount,
            memberCount,
            resourceCount,
            storageUsedBytes,
            limits.storageLimitBytes(),
            limits.vaultLimit(),
            limits.memberLimit(),
            storageUsedBytes < limits.storageLimitBytes(),
            vaultCount < limits.vaultLimit(),
            memberCount < limits.memberLimit()
        );
    }

    @Transactional(readOnly = true)
    public void requireCanCreateVault(Workspace workspace) {
        WorkspaceUsageResponse usage = usage(workspace);
        if (!usage.canCreateVault()) {
            throw new BadRequestException(ErrorCode.QUOTA_EXCEEDED, "Workspace vault limit reached");
        }
    }

    @Transactional(readOnly = true)
    public void requireCanAddMember(Workspace workspace) {
        WorkspaceUsageResponse usage = usage(workspace);
        if (!usage.canInviteMember()) {
            throw new BadRequestException(ErrorCode.QUOTA_EXCEEDED, "Workspace member limit reached");
        }
    }

    @Transactional(readOnly = true)
    public void requireCanUpload(Workspace workspace, long bytesToAdd) {
        WorkspaceUsageResponse usage = usage(workspace);
        if (usage.storageUsedBytes() + Math.max(0, bytesToAdd) > usage.storageLimitBytes()) {
            throw new BadRequestException(ErrorCode.QUOTA_EXCEEDED, "Workspace storage limit reached");
        }
    }

    public WorkspacePlanLimits limitsFor(WorkspacePlan plan) {
        return switch (plan == null ? WorkspacePlan.FREE : plan) {
            case FREE -> new WorkspacePlanLimits(5, 3, 100 * MIB);
            case PRO -> new WorkspacePlanLimits(50, 10, 5 * 1024 * MIB);
            case TEAM -> new WorkspacePlanLimits(500, 100, 100 * 1024 * MIB);
        };
    }
}

