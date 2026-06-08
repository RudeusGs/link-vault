package com.linkvault.workspaces.dto;

import com.linkvault.workspaces.enums.WorkspacePlan;
import com.linkvault.workspaces.enums.WorkspaceRole;
import java.time.Instant;
import java.util.UUID;

public record WorkspaceResponse(
    UUID id,
    String name,
    String slug,
    UUID ownerUserId,
    WorkspacePlan plan,
    WorkspaceRole role,
    Instant createdAt,
    Instant updatedAt
) {
}