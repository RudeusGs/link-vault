package com.linkvault.auth.dto;

import com.linkvault.auth.entity.RefreshToken;
import com.linkvault.users.entity.User;

public record AuthResponse(
    String accessToken,
    String tokenType,
    long expiresInSeconds,
    UserResponse user,
    String refreshToken
) {
}