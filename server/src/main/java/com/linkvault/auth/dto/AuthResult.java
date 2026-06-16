package com.linkvault.auth.dto;

public record AuthResult(
    AuthResponse response,
    String refreshToken
) {
}
