package com.fleetcontrol.msfuel.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Metadatos de la documentación OpenAPI y esquema de autenticación por token. */
@Configuration
public class OpenApiConfig {

  private static final String BEARER_SCHEME = "bearerAuth";

  /** Documentación de ms-fuel con el esquema Bearer para el JWT. */
  @Bean
  public OpenAPI msFuelApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("FleetControl - ms-fuel")
                .version("v1")
                .description("Repostajes de la flota y cálculo de consumo"))
        .components(
            new Components()
                .addSecuritySchemes(
                    BEARER_SCHEME,
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
  }
}
