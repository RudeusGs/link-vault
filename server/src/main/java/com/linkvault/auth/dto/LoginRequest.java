package com.linkvault.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
    @NotBlank @Size(min = 3, max = 100) String username,
    @NotBlank @Size(max = 72) String password
) {
}