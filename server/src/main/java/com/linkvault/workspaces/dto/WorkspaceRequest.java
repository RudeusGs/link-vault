package com.linkvault.workspaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WorkspaceRequest(
    @NotBlank @Size(max = 150) String name
) {
}