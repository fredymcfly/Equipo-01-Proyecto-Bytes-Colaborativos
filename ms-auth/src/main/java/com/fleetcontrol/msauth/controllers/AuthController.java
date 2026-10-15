package com.fleetcontrol.msauth.controllers;

import com.fleetcontrol.msauth.constants.ApiPaths;
import com.fleetcontrol.msauth.constants.OpenApiConstants;
import com.fleetcontrol.msauth.models.dto.request.LoginRequest;
import com.fleetcontrol.msauth.models.dto.request.RegisterRequest;
import com.fleetcontrol.msauth.models.dto.response.ErrorResponse;
import com.fleetcontrol.msauth.models.dto.response.LoginResponse;
import com.fleetcontrol.msauth.models.dto.response.RegisterResponse;
import com.fleetcontrol.msauth.models.dto.response.ValidateResponse;
import com.fleetcontrol.msauth.services.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** REST endpoints for registration, login and token validation. */
@RestController
@RequestMapping(ApiPaths.Auth.BASE)
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Registration, login and token validation")
public class AuthController {

  private final AuthService authService;

  /**
   * Registers a new user with the {@code MANAGER} role.
   *
   * @param request the registration data
   * @return the created user
   */
  @Operation(summary = "Registers a new user with the MANAGER role")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      description = "Information required to create a new account.",
      required = true,
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = RegisterRequest.class)))
  @ApiResponses({
    @ApiResponse(
        responseCode = OpenApiConstants.Code.CREATED,
        description = "Account created successfully.",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = RegisterResponse.class))),
    @ApiResponse(
        responseCode = OpenApiConstants.Code.BAD_REQUEST,
        description = "Invalid registration data.",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = OpenApiConstants.Code.CONFLICT,
        description = "The username or email address is already in use.",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class)))
  })
  @PostMapping(ApiPaths.Auth.REGISTER)
  @ResponseStatus(HttpStatus.CREATED)
  public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
    return authService.register(request);
  }

  /**
   * Authenticates a user and returns a JWT.
   *
   * @param request the email and password
   * @return the token and its metadata
   */
  @Operation(summary = "Authenticates a user and returns a JWT")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      description = "Email and password of the account.",
      required = true,
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = LoginRequest.class)))
  @ApiResponses({
    @ApiResponse(
        responseCode = OpenApiConstants.Code.SUCCESS,
        description = "Login successful. Returns the access token.",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = LoginResponse.class))),
    @ApiResponse(
        responseCode = OpenApiConstants.Code.BAD_REQUEST,
        description = "Invalid login data.",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = OpenApiConstants.Code.UNAUTHORIZED,
        description = "The email or the password is incorrect.",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class)))
  })
  @PostMapping(ApiPaths.Auth.LOGIN)
  public LoginResponse login(@Valid @RequestBody LoginRequest request) {
    return authService.login(request);
  }

  /**
   * Validates the token sent in the {@code Authorization} header.
   *
   * @param authorization the raw header, or null if it was not sent
   * @return 200 with the user data, or 401 with {@code valid} set to false
   */
  @Operation(
      summary = "Validates a JWT",
      security = @SecurityRequirement(name = OpenApiConstants.BEARER_AUTH))
  @ApiResponses({
    @ApiResponse(
        responseCode = OpenApiConstants.Code.SUCCESS,
        description = "The token is valid. Returns the user data.",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ValidateResponse.class))),
    @ApiResponse(
        responseCode = OpenApiConstants.Code.UNAUTHORIZED,
        description = "The token is missing, invalid or expired.",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ValidateResponse.class)))
  })
  @PostMapping(ApiPaths.Auth.VALIDATE)
  public ResponseEntity<ValidateResponse> validate(
      @Parameter(hidden = true) @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
          String authorization) {
    ValidateResponse response = authService.validate(authorization);
    HttpStatus status = response.valid() ? HttpStatus.OK : HttpStatus.UNAUTHORIZED;
    return ResponseEntity.status(status).body(response);
  }
}
