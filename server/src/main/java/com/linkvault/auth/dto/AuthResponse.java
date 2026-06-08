package com.linkvault.auth.dto;

public record AuthResponse(
    String accessToken,
    String tokenType,
    long expiresInSeconds,
    UserResponse user,
    String refreshToken
) {
}