package com.resourcemagnate.app.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI resourceMagnateOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Resource Magnate API")
                .description("Backend API for the Resource Magnate idle game")
                .version("1.0"));
    }
}
