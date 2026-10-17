package com.fleetcontrol.msalerts.config;

import com.fleetcontrol.msalerts.constants.SecurityConstants;
import com.fleetcontrol.msalerts.constants.SwaggerConstants;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** OpenAPI metadata and the schemes offered by the Authorize button of Swagger UI. */
@Configuration
public class OpenApiConfig {

  /** Describes the API and declares the JWT and internal key schemes. */
  @Bean
  public OpenAPI openApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title(SwaggerConstants.Api.TITLE)
                .description(SwaggerConstants.Api.DESCRIPTION)
                .version(SwaggerConstants.Api.VERSION))
        .components(
            new Components()
                .addSecuritySchemes(
                    SwaggerConstants.Security.BEARER,
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme(SwaggerConstants.Security.BEARER_SCHEME)
                        .bearerFormat(SwaggerConstants.Security.BEARER_FORMAT)
                        .description(SwaggerConstants.Security.BEARER_DESCRIPTION))
                .addSecuritySchemes(
                    SwaggerConstants.Security.INTERNAL_KEY,
                    new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .name(SecurityConstants.INTERNAL_KEY_HEADER)
                        .description(SwaggerConstants.Security.INTERNAL_KEY_DESCRIPTION)));
  }
}
