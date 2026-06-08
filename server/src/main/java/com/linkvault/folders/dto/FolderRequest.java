package com.linkvault.folders.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FolderRequest(
    @NotBlank @Size(max = 150) String name,
    @Size(max = 1000) String description,
    @Size(max = 80) String icon,
    Integer sortOrder
) {
}