package com.fleetcontrol.msauth.config;

import com.fleetcontrol.msauth.constants.OpenApiConstants;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/** Configuration class for OpenAPI (Swagger) documentation. */
@Configuration
@OpenAPIDefinition(
    info =
        @Info(
            title = "FleetControl - ms-auth",
            description = "Registration, login and JWT validation",
            version = "1.0"))
@SecurityScheme(
    name = OpenApiConstants.BEARER_AUTH,
    description = "Authorization header with the JWT",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT")
public class OpenApiConfig {}
