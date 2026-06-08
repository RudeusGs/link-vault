package com.linkvault.workspaces.dto;

import com.linkvault.workspaces.enums.InvitationStatus;
import com.linkvault.workspaces.enums.WorkspaceRole;
import java.time.Instant;
import java.util.UUID;

public record WorkspaceInvitationResponse(
    UUID id,
    UUID workspaceId,
    String invitedIdentifier,
    UUID invitedUserId,
    UUID invitedByUserId,
    WorkspaceRole role,
    String token,
    String acceptPath,
    InvitationStatus status,
    Instant expiresAt,
    Instant acceptedAt,
    Instant createdAt,
    Instant updatedAt
) {
}