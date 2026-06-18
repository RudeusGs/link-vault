package com.linkvault.workspaces.dto;

import com.linkvault.workspaces.enums.WorkspacePlan;
import jakarta.validation.constraints.NotNull;

public record WorkspacePlanRequest(
    @NotNull(message = "Plan is required")
    WorkspacePlan plan
) {
}
