package com.linkvault.auth;

import com.linkvault.auth.dto.AuthAvailabilityResponse;
import com.linkvault.auth.dto.AuthResponse;
import com.linkvault.auth.dto.LoginRequest;
import com.linkvault.auth.dto.RegisterRequest;
import com.linkvault.auth.dto.UserResponse;
import com.linkvault.auth.security.JwtService;
import com.linkvault.common.exception.BadRequestException;
import com.linkvault.common.exception.UnauthorizedException;
import com.linkvault.users.User;
import com.linkvault.users.UserContextService;
import com.linkvault.users.UserRepository;
import java.time.Instant;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserContextService userContextService;

    public AuthService(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        JwtService jwtService,
        UserContextService userContextService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userContextService = userContextService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
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
        return toAuthResponse(savedUser);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String username = normalizeUsername(request.username());
        User user = userRepository.findByUsernameIgnoreCase(username)
            .orElseThrow(() -> new UnauthorizedException("Username or password is incorrect"));

        if (Boolean.FALSE.equals(user.getIsEnabled())) {
            throw new UnauthorizedException("Account is disabled");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Username or password is incorrect");
        }

        user.setLastLoginAt(Instant.now());
        return toAuthResponse(userRepository.save(user));
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

    private AuthResponse toAuthResponse(User user) {
        return new AuthResponse(
            jwtService.generateAccessToken(user),
            "Bearer",
            jwtService.expiresInSeconds(),
            toUserResponse(user)
        );
    }

    private String resolveDisplayName(String displayName, String username) {
        String value = trimToNull(displayName);
        return value == null ? username : value;
    }

    private String normalizeEmail(String email) {
        if (isBlank(email)) {
            return "";
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeUsername(String username) {
        if (isBlank(username)) {
            return "";
        }
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (isBlank(value)) {
            return null;
        }
        return value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
