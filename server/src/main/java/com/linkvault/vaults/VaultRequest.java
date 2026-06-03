package com.linkvault.vaults;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VaultRequest(
    @NotBlank @Size(max = 150) String name,
    @Size(max = 1000) String description,
    @Size(max = 80) String icon,
    @Size(max = 40) String color
) {
}
