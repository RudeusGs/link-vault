package com.linkvault.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank @Email @Size(max = 255) String email,
    @NotBlank @Size(min = 6, max = 72) String password,
    @NotBlank @Size(min = 3, max = 100) String username,
    @Size(max = 150) String displayName,
    @Size(max = 500) String avatarUrl
) {
}