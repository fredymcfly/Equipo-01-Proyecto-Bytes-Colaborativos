package com.fleetcontrol.msdrivers.documentation;

import com.fleetcontrol.msdrivers.constants.SwaggerConstants;
import com.fleetcontrol.msdrivers.models.dto.response.ErrorResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.http.MediaType;

/**
 * Documents the errors that every endpoint can return: invalid input, missing or invalid
 * credentials and insufficient role. Put it next to the responses specific to the endpoint.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@ApiResponses({
  @ApiResponse(
      responseCode = SwaggerConstants.Status.BAD_REQUEST,
      description = SwaggerConstants.Responses.BAD_REQUEST,
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = ErrorResponse.class),
              examples = @ExampleObject(value = SwaggerConstants.Examples.ERROR_VALIDATION))),
  @ApiResponse(
      responseCode = SwaggerConstants.Status.UNAUTHORIZED,
      description = SwaggerConstants.Responses.UNAUTHORIZED,
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = ErrorResponse.class),
              examples = @ExampleObject(value = SwaggerConstants.Examples.ERROR_UNAUTHORIZED))),
  @ApiResponse(
      responseCode = SwaggerConstants.Status.FORBIDDEN,
      description = SwaggerConstants.Responses.FORBIDDEN,
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = ErrorResponse.class),
              examples = @ExampleObject(value = SwaggerConstants.Examples.ERROR_FORBIDDEN)))
})
public @interface CommonErrorResponses {}
