package com.linkvault.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkvault.users.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private JwtProperties jwtProperties;
    private User user;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        jwtProperties.setSecret("this-is-a-very-long-secret-key-for-testing-purposes-only");
        jwtProperties.setIssuer("test-issuer");
        jwtProperties.setExpirationMinutes(60);

        jwtService = new JwtService(jwtProperties, new ObjectMapper());

        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test@example.com");
        user.setUsername("testuser");
    }

    @Test
    void shouldGenerateAndParseToken() {
        String token = jwtService.generateAccessToken(user);
        assertNotNull(token);

        Optional<JwtClaims> claims = jwtService.parse(token);
        assertTrue(claims.isPresent());
        assertEquals(user.getId(), claims.get().userId());
        assertEquals(user.getEmail(), claims.get().email());
        assertEquals(user.getUsername(), claims.get().username());
    }

    @Test
    void shouldRejectMalformedToken() {
        Optional<JwtClaims> claims = jwtService.parse("invalid.token.here");
        assertFalse(claims.isPresent());
    }

    @Test
    void shouldRejectTamperedToken() {
        String token = jwtService.generateAccessToken(user);
        String[] parts = token.split("\\.");
        String tamperedToken = parts[0] + "." + parts[1] + "tampered." + parts[2];

        Optional<JwtClaims> claims = jwtService.parse(tamperedToken);
        assertFalse(claims.isPresent());
    }

    @Test
    void shouldRejectExpiredToken() {
        jwtProperties.setExpirationMinutes(-1); // Immediately expires
        String token = jwtService.generateAccessToken(user);

        Optional<JwtClaims> claims = jwtService.parse(token);
        assertFalse(claims.isPresent());
    }
}
