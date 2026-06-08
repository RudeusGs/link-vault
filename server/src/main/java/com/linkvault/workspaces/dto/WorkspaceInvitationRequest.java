package com.linkvault.workspaces.dto;

import com.linkvault.workspaces.enums.WorkspaceRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WorkspaceInvitationRequest(
    @NotBlank @Size(max = 255) String invitedIdentifier,
    WorkspaceRole role
) {
}