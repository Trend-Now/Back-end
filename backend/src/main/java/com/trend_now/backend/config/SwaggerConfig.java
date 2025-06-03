package com.trend_now.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    private static final String SECURITY_SCHEME_NAME = "Bearer Authentication";
    private static final String JWT_FORMAT = "Bearer";
    private static final String TOKEN = "JWT";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(info)
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, createBearerScheme()));
    }

    Info info = new Info()
            .title("Trend Now Swagger")
            .version("0.0.1")
            .description(
            "<h3>Trend Now API Test</h3>");

    private SecurityScheme createBearerScheme() {
        return new SecurityScheme()
                .name(SECURITY_SCHEME_NAME)
                .type(SecurityScheme.Type.HTTP)
                .scheme(JWT_FORMAT)
                .bearerFormat(TOKEN)
                .in(SecurityScheme.In.HEADER);
    }
}

