package com.linkvault.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI linkVaultOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title("LinkVault API")
                .version("v1")
                .description("Personal Resource Hub API for vaults, folders, resources, tags, storage, and dashboard.")
                .license(new License().name("Internal")));
    }
}
