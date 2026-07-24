package com.linkvault.auth.controller;

import com.linkvault.auth.dto.AuthAvailabilityResponse;
import com.linkvault.auth.dto.AuthResponse;
import com.linkvault.auth.dto.AuthResult;
import com.linkvault.auth.dto.LoginRequest;
import com.linkvault.auth.dto.RegisterRequest;
import com.linkvault.auth.dto.UserResponse;
import com.linkvault.auth.dto.UserSessionResponse;
import com.linkvault.auth.service.AuthService;
import com.linkvault.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
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
        HttpServletRequest servletRequest,
        HttpServletResponse servletResponse
    ) {
        AuthResult result = authService.register(request, userAgent(servletRequest), ipAddress(servletRequest));
        setRefreshTokenCookie(servletResponse, result.refreshToken());
        return ApiResponse.success("Account registered", result.response());
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(
        @Valid @RequestBody LoginRequest request, 
        HttpServletRequest servletRequest,
        HttpServletResponse servletResponse
    ) {
        AuthResult result = authService.login(request, userAgent(servletRequest), ipAddress(servletRequest));
        setRefreshTokenCookie(servletResponse, result.refreshToken());
        return ApiResponse.success("Logged in", result.response());
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(
        @org.springframework.web.bind.annotation.CookieValue(name = "refresh_token", required = false) String refreshToken,
        HttpServletRequest servletRequest,
        HttpServletResponse servletResponse
    ) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new com.linkvault.common.exception.UnauthorizedException(com.linkvault.common.exception.ErrorCode.AUTH_LOGIN_REQUIRED, "Refresh token is missing");
        }
        AuthResult result = authService.refresh(refreshToken, userAgent(servletRequest), ipAddress(servletRequest));
        setRefreshTokenCookie(servletResponse, result.refreshToken());
        return ApiResponse.success("Token refreshed", result.response());
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(
        @org.springframework.web.bind.annotation.CookieValue(name = "refresh_token", required = false) String refreshToken,
        HttpServletResponse servletResponse
    ) {
        authService.logout(refreshToken);
        clearRefreshTokenCookie(servletResponse);
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

    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from("refresh_token", refreshToken)
            .httpOnly(true)
            .secure(true)
            .sameSite("Strict")
            .path("/api/auth")
            .maxAge(Duration.ofDays(30))
            .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("refresh_token", "")
            .httpOnly(true)
            .secure(true)
            .sameSite("Strict")
            .path("/api/auth")
            .maxAge(0)
            .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}