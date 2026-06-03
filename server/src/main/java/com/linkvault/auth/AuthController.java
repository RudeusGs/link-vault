package com.linkvault.auth;

import com.linkvault.auth.dto.AuthAvailabilityResponse;
import com.linkvault.auth.dto.AuthResponse;
import com.linkvault.auth.dto.LoginRequest;
import com.linkvault.auth.dto.RegisterRequest;
import com.linkvault.auth.dto.UserResponse;
import com.linkvault.common.response.ApiResponse;
import jakarta.validation.Valid;
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
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success("Account registered", authService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success("Logged in", authService.login(request));
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
}
