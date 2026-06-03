package com.linkvault.auth.dto;

public record AuthAvailabilityResponse(
    boolean emailAvailable,
    boolean usernameAvailable
) {
}
