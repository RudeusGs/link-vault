package com.linkvault.auth.dto;

import com.linkvault.auth.entity.RefreshToken;
import jakarta.validation.constraints.NotBlank;

public record LogoutRequest(
    @NotBlank String refreshToken
) {
}