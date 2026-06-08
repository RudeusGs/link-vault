package com.linkvault.auth.service;

import com.linkvault.users.entity.User;

public record RefreshTokenResult(
    User user,
    String refreshToken
) {
}