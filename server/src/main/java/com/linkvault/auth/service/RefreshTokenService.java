package com.linkvault.auth.service;

import com.linkvault.auth.dto.UserSessionResponse;
import com.linkvault.auth.entity.RefreshToken;
import com.linkvault.auth.repository.RefreshTokenRepository;
import com.linkvault.common.exception.ErrorCode;
import com.linkvault.common.exception.NotFoundException;
import com.linkvault.common.exception.UnauthorizedException;
import com.linkvault.users.entity.User;
import com.linkvault.users.service.UserContextService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserContextService userContextService;

    public RefreshTokenService(
        RefreshTokenRepository refreshTokenRepository,
        UserContextService userContextService
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userContextService = userContextService;
    }

    @Transactional
    public String createForUser(User user, String userAgent, String ipAddress) {
        String rawToken = randomToken();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(hash(rawToken));
        refreshToken.setExpiresAt(Instant.now().plus(30, ChronoUnit.DAYS));
        refreshToken.setLastUsedAt(Instant.now());
        refreshToken.setUserAgent(trim(userAgent, 500));
        refreshToken.setIpAddress(trim(ipAddress, 80));
        refreshTokenRepository.save(refreshToken);

        return rawToken;
    }

    @Transactional
    public RefreshTokenResult rotate(String rawToken, String userAgent, String ipAddress) {
        RefreshToken current = validToken(rawToken);
        current.setRevokedAt(Instant.now());
        current.setLastUsedAt(Instant.now());
        refreshTokenRepository.save(current);

        String nextToken = createForUser(current.getUser(), userAgent, ipAddress);
        return new RefreshTokenResult(current.getUser(), nextToken);
    }

    @Transactional
    public void revoke(String rawToken) {
        RefreshToken refreshToken = validToken(rawToken);
        refreshToken.setRevokedAt(Instant.now());
        refreshToken.setLastUsedAt(Instant.now());
        refreshTokenRepository.save(refreshToken);
    }

    @Transactional(readOnly = true)
    public List<UserSessionResponse> listCurrentUserSessions() {
        User user = userContextService.getCurrentUser();
        return refreshTokenRepository.findByUser_IdOrderByCreatedAtDesc(user.getId()).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public void revokeSession(UUID sessionId) {
        User user = userContextService.getCurrentUser();
        RefreshToken refreshToken = refreshTokenRepository.findByIdAndUser_Id(sessionId, user.getId())
            .orElseThrow(() -> new NotFoundException(ErrorCode.REFRESH_TOKEN_INVALID, "Session not found"));
        refreshToken.setRevokedAt(Instant.now());
        refreshTokenRepository.save(refreshToken);
    }

    private RefreshToken validToken(String rawToken) {
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(hash(rawToken))
            .orElseThrow(() -> new UnauthorizedException(ErrorCode.REFRESH_TOKEN_INVALID, "Refresh token is invalid"));

        if (refreshToken.getRevokedAt() != null) {
            throw new UnauthorizedException(ErrorCode.REFRESH_TOKEN_INVALID, "Refresh token has been revoked");
        }

        if (refreshToken.getExpiresAt().isBefore(Instant.now())) {
            throw new UnauthorizedException(ErrorCode.REFRESH_TOKEN_EXPIRED, "Refresh token has expired");
        }

        return refreshToken;
    }

    private UserSessionResponse toResponse(RefreshToken refreshToken) {
        return new UserSessionResponse(
            refreshToken.getId(),
            refreshToken.getCreatedAt(),
            refreshToken.getExpiresAt(),
            refreshToken.getRevokedAt(),
            refreshToken.getLastUsedAt(),
            refreshToken.getUserAgent(),
            refreshToken.getIpAddress()
        );
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new UnauthorizedException("Could not process refresh token");
        }
    }

    private String trim(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }
}