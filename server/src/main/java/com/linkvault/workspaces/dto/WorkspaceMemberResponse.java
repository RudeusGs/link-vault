package com.linkvault.workspaces.dto;

import com.linkvault.workspaces.enums.WorkspaceRole;
import java.time.Instant;
import java.util.UUID;

public record WorkspaceMemberResponse(
    UUID id,
    UUID userId,
    String username,
    String email,
    String displayName,
    WorkspaceRole role,
    Instant joinedAt
) {
}