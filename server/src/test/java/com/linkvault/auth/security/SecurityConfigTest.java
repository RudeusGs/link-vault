package com.linkvault.auth.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.junit.jupiter.api.BeforeEach;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@SpringBootTest
class SecurityConfigTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void shouldAllowAccessToPublicEndpoints() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/auth/availability?email=test@example.com"))
                .andExpect(status().isOk());

        // We expect a 400 Bad Request because no body is provided, not 401
        mockMvc.perform(post("/api/auth/login"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldDenyAccessToProtectedEndpointsWithoutToken() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/auth/sessions"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/resources"))
                .andExpect(status().isUnauthorized());
    }
}
