package com.linkvault.auth.controller;

import com.linkvault.auth.dto.AuthAvailabilityResponse;
import com.linkvault.auth.dto.AuthResponse;
import com.linkvault.auth.dto.LoginRequest;
import com.linkvault.auth.dto.LogoutRequest;
import com.linkvault.auth.dto.RefreshTokenRequest;
import com.linkvault.auth.dto.RegisterRequest;
import com.linkvault.auth.dto.UserResponse;
import com.linkvault.auth.dto.UserSessionResponse;
import com.linkvault.auth.service.AuthService;
import com.linkvault.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(
        @Valid @RequestBody RegisterRequest request,
        HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(
            "Account registered",
            authService.register(request, userAgent(servletRequest), ipAddress(servletRequest))
        );
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        return ApiResponse.success(
            "Logged in",
            authService.login(request, userAgent(servletRequest), ipAddress(servletRequest))
        );
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(
        @Valid @RequestBody RefreshTokenRequest request,
        HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(
            "Token refreshed",
            authService.refresh(request, userAgent(servletRequest), ipAddress(servletRequest))
        );
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request);
        return ApiResponse.success("Logged out", null);
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> me() {
        return ApiResponse.success("Current user loaded", authService.me());
    }

    @GetMapping("/availability")
    public ApiResponse<AuthAvailabilityResponse> availability(
        @RequestParam(required = false) String email,
        @RequestParam(required = false) String username
    ) {
        return ApiResponse.success("Availability checked", authService.checkAvailability(email, username));
    }

    @GetMapping("/sessions")
    public ApiResponse<List<UserSessionResponse>> sessions() {
        return ApiResponse.success("Sessions loaded", authService.sessions());
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ApiResponse<Void> revokeSession(@org.springframework.web.bind.annotation.PathVariable UUID sessionId) {
        authService.revokeSession(sessionId);
        return ApiResponse.success("Session revoked", null);
    }

    private String userAgent(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }

    private String ipAddress(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}