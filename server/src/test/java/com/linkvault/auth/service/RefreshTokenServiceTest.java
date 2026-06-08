package com.linkvault.auth.service;

import com.linkvault.auth.entity.RefreshToken;
import com.linkvault.auth.repository.RefreshTokenRepository;
import com.linkvault.common.exception.UnauthorizedException;
import com.linkvault.users.entity.User;
import com.linkvault.users.service.UserContextService;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RefreshTokenServiceTest {

    private final RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
    private final UserContextService userContextService = mock(UserContextService.class);
    private final RefreshTokenService refreshTokenService = new RefreshTokenService(
        refreshTokenRepository,
        userContextService
    );

    @Test
    void rotateRevokesOldTokenAndCreatesReplacement() {
        User user = user();
        RefreshToken existing = refreshToken(user);

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(existing));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RefreshTokenResult result = refreshTokenService.rotate("refresh-token", "agent", "127.0.0.1");

        assertThat(result.user()).isSameAs(user);
        assertThat(result.refreshToken()).isNotBlank();
        assertThat(existing.getRevokedAt()).isNotNull();
        verify(refreshTokenRepository, atLeast(2)).save(any(RefreshToken.class));
    }

    @Test
    void revokedTokenIsRejected() {
        RefreshToken existing = refreshToken(user());
        existing.setRevokedAt(Instant.now());

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> refreshTokenService.rotate("refresh-token", null, null))
            .isInstanceOf(UnauthorizedException.class);
    }

    private RefreshToken refreshToken(User user) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setId(UUID.randomUUID());
        refreshToken.setUser(user);
        refreshToken.setTokenHash("hash");
        refreshToken.setExpiresAt(Instant.now().plusSeconds(3600));
        return refreshToken;
    }

    private User user() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("ada");
        user.setEmail("ada@example.com");
        return user;
    }
}