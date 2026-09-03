package org.geoverity.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("GeoVerity Secure Authentication API")
                        .version("1.0.0")
                        .description("High-security mobile-based digital evidence authentication platform API.\n\n"
                                + "Features: Server-side ECDSA P-256 signing, SHA-256 composite hashing, "
                                + "monotonic offline time reconciliation, and third-party image verification without login.")
                        .contact(new Contact().name("GeoVerity Security Engineering").email("security@geoverity.org"))
                        .license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0")))
                .addSecurityItem(new SecurityRequirement().addList("ApiKeyAuth").addList("AdminKeyAuth"))
                .components(new Components()
                        .addSecuritySchemes("ApiKeyAuth", new SecurityScheme()
                                .name("X-API-Key")
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .description("Client API Key for Android Application Authorization"))
                        .addSecuritySchemes("AdminKeyAuth", new SecurityScheme()
                                .name("X-Admin-Key")
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .description("Master Admin Key for Administrative Management")));
    }
}
