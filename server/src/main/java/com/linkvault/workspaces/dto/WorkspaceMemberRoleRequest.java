package com.linkvault.workspaces.dto;

import com.linkvault.workspaces.enums.WorkspaceRole;
import jakarta.validation.constraints.NotNull;

public record WorkspaceMemberRoleRequest(
    @NotNull WorkspaceRole role
) {
}