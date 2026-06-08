package com.linkvault.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI linkVaultOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title("LinkVault API")
                .version("v1")
                .description("Personal Resource Hub API for vaults, folders, resources, tags, storage, and dashboard.")
                .license(new License().name("Internal")))
            .components(new Components()
                .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .name("Authorization")
                    .in(SecurityScheme.In.HEADER)))
            .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }
}