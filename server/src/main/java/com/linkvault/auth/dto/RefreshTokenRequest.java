package com.linkvault.auth.dto;

import com.linkvault.auth.entity.RefreshToken;
import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
    @NotBlank String refreshToken
) {
}