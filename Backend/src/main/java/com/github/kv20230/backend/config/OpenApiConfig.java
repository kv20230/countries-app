package com.github.kv20230.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.context.annotation.Bean;
import io.swagger.v3.oas.models.info.Info;

public class OpenApiConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("REST Countries API")
                        .version("1.0.0")
                        .description("Backend service providing country data mapped from REST Countries v5 with caching."));
    }
}
