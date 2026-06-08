package com.linkvault.auth.service;

import com.linkvault.auth.dto.AuthAvailabilityResponse;
import com.linkvault.auth.dto.AuthResponse;
import com.linkvault.auth.dto.LoginRequest;
import com.linkvault.auth.dto.LogoutRequest;
import com.linkvault.auth.dto.RefreshTokenRequest;
import com.linkvault.auth.dto.RegisterRequest;
import com.linkvault.auth.dto.UserResponse;
import com.linkvault.auth.dto.UserSessionResponse;
import com.linkvault.auth.security.JwtService;
import com.linkvault.common.exception.BadRequestException;
import com.linkvault.common.exception.ErrorCode;
import com.linkvault.common.exception.UnauthorizedException;
import com.linkvault.common.util.TextSanitizer;
import com.linkvault.users.entity.User;
import com.linkvault.users.repository.UserRepository;
import com.linkvault.users.service.UserContextService;
import com.linkvault.workspaces.service.WorkspaceService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserContextService userContextService;
    private final WorkspaceService workspaceService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        JwtService jwtService,
        UserContextService userContextService,
        WorkspaceService workspaceService,
        RefreshTokenService refreshTokenService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userContextService = userContextService;
        this.workspaceService = workspaceService;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        return register(request, null, null);
    }

    @Transactional
    public AuthResponse register(RegisterRequest request, String userAgent, String ipAddress) {
        String email = normalizeEmail(request.email());
        String username = normalizeUsername(request.username());

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BadRequestException("Email is already registered");
        }

        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new BadRequestException("Username is already taken");
        }

        User user = new User();
        user.setEmail(email);
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setDisplayName(resolveDisplayName(request.displayName(), username));
        user.setAvatarUrl(trimToNull(request.avatarUrl()));
        user.setIsVerified(true);
        user.setAuthProvider("LOCAL");
        user.setLastLoginAt(Instant.now());

        User savedUser = userRepository.save(user);
        workspaceService.createDefaultWorkspaceForUser(savedUser);
        return toAuthResponse(savedUser, refreshTokenService.createForUser(savedUser, userAgent, ipAddress));
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        return login(request, null, null);
    }

    @Transactional
    public AuthResponse login(LoginRequest request, String userAgent, String ipAddress) {
        String username = normalizeUsername(request.username());
        User user = userRepository.findByUsernameIgnoreCase(username)
            .orElseThrow(() -> new UnauthorizedException(
                ErrorCode.AUTH_INVALID_CREDENTIALS,
                "Username or password is incorrect"
            ));

        if (Boolean.FALSE.equals(user.getIsEnabled())) {
            throw new UnauthorizedException("Account is disabled");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException(ErrorCode.AUTH_INVALID_CREDENTIALS, "Username or password is incorrect");
        }

        user.setLastLoginAt(Instant.now());
        User savedUser = userRepository.save(user);
        return toAuthResponse(savedUser, refreshTokenService.createForUser(savedUser, userAgent, ipAddress));
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request, String userAgent, String ipAddress) {
        RefreshTokenResult result = refreshTokenService.rotate(request.refreshToken(), userAgent, ipAddress);
        return toAuthResponse(result.user(), result.refreshToken());
    }

    @Transactional
    public void logout(LogoutRequest request) {
        refreshTokenService.revoke(request.refreshToken());
    }

    @Transactional(readOnly = true)
    public List<UserSessionResponse> sessions() {
        return refreshTokenService.listCurrentUserSessions();
    }

    @Transactional
    public void revokeSession(UUID sessionId) {
        refreshTokenService.revokeSession(sessionId);
    }

    @Transactional(readOnly = true)
    public UserResponse me() {
        return toUserResponse(userContextService.getCurrentUser());
    }

    @Transactional(readOnly = true)
    public AuthAvailabilityResponse checkAvailability(String email, String username) {
        boolean emailAvailable = isBlank(email) || !userRepository.existsByEmailIgnoreCase(normalizeEmail(email));
        boolean usernameAvailable = isBlank(username)
            || !userRepository.existsByUsernameIgnoreCase(normalizeUsername(username));
        return new AuthAvailabilityResponse(emailAvailable, usernameAvailable);
    }

    public UserResponse toUserResponse(User user) {
        return new UserResponse(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getDisplayName(),
            user.getAvatarUrl(),
            user.getIsVerified(),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }

    private AuthResponse toAuthResponse(User user, String refreshToken) {
        return new AuthResponse(
            jwtService.generateAccessToken(user),
            "Bearer",
            jwtService.expiresInSeconds(),
            toUserResponse(user),
            refreshToken
        );
    }

    private String resolveDisplayName(String displayName, String username) {
        String value = trimToNull(displayName);
        return value == null ? username : value;
    }

    private String normalizeEmail(String email) {
        return TextSanitizer.lowerTrimmed(email);
    }

    private String normalizeUsername(String username) {
        return TextSanitizer.lowerTrimmed(username);
    }

    private String trimToNull(String value) {
        return TextSanitizer.trimToNull(value);
    }

    private boolean isBlank(String value) {
        return TextSanitizer.isBlank(value);
    }
}