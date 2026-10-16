package com.fleetcontrol.msroutes.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/** Describes the ms-routes API and the JWT bearer scheme shown in Swagger UI. */
@Configuration
@OpenAPIDefinition(
    info =
        @Info(
            title = "ms-routes API",
            version = "0.0.1",
            description = "Planning, execution and history of fleet routes"))
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT")
public class OpenApiConfig {}
