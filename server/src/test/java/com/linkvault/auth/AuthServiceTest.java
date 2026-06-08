package com.linkvault.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkvault.auth.dto.AuthResponse;
import com.linkvault.auth.dto.LoginRequest;
import com.linkvault.auth.dto.RegisterRequest;
import com.linkvault.auth.security.JwtProperties;
import com.linkvault.auth.security.JwtService;
import com.linkvault.common.exception.ErrorCode;
import com.linkvault.common.exception.UnauthorizedException;
import com.linkvault.users.User;
import com.linkvault.users.UserContextService;
import com.linkvault.users.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final UserContextService userContextService = mock(UserContextService.class);
    private final AuthService authService = new AuthService(
        userRepository,
        passwordEncoder,
        jwtService(),
        userContextService
    );

    @Test
    void registerNormalizesIdentityAndReturnsBearerToken() {
        when(userRepository.existsByEmailIgnoreCase("ada@example.com")).thenReturn(false);
        when(userRepository.existsByUsernameIgnoreCase("ada")).thenReturn(false);
        when(passwordEncoder.encode("secret-password")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });

        AuthResponse response = authService.register(new RegisterRequest(
            " Ada@Example.COM ",
            "secret-password",
            " ADA ",
            "Ada Lovelace",
            null
        ));

        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.user().username()).isEqualTo("ada");
        assertThat(response.user().email()).isEqualTo("ada@example.com");
    }

    @Test
    void loginRejectsInvalidPasswordWithAuthErrorCode() {
        User user = new User();
        user.setUsername("ada");
        user.setEmail("ada@example.com");
        user.setPasswordHash("hashed-password");
        user.setIsEnabled(true);

        when(userRepository.findByUsernameIgnoreCase("ada")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "hashed-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest(" ada ", "wrong-password")))
            .isInstanceOfSatisfying(UnauthorizedException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTH_INVALID_CREDENTIALS)
            );
    }

    private JwtService jwtService() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("unit-test-jwt-secret-that-is-long-enough");
        properties.setIssuer("link-vault-test");
        return new JwtService(properties, new ObjectMapper());
    }
}
