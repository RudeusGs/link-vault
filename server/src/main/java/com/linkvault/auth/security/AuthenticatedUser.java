package com.linkvault.auth.security;

import java.util.UUID;

public record AuthenticatedUser(
    UUID id,
    String email,
    String username
) {
}
