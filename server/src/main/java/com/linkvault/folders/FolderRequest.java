package com.linkvault.folders;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record FolderRequest(
    @NotNull UUID vaultId,
    UUID parentId,
    @NotBlank @Size(max = 150) String name,
    @Size(max = 1000) String description,
    @Size(max = 80) String icon,
    Integer sortOrder
) {
}
