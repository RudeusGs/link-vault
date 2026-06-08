package com.linkvault.workspaces.dto;

import com.linkvault.workspaces.enums.WorkspacePlan;

public record WorkspaceUsageResponse(
    WorkspacePlan currentPlan,
    long vaultCount,
    long memberCount,
    long resourceCount,
    long storageUsedBytes,
    long storageLimitBytes,
    long vaultLimit,
    long memberLimit,
    boolean canUpload,
    boolean canCreateVault,
    boolean canInviteMember
) {
}