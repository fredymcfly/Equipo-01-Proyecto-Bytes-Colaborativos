package com.fleetcontrol.msauth.models.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fleetcontrol.msauth.models.enums.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/**
 * Response of {@code POST /api/auth/validate}. Fields that do not apply are omitted: a valid token
 * returns the user data, an invalid one returns {@code error}, {@code message} and {@code
 * timestamp}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ValidateResponse(
    @Schema(example = "true") boolean valid,
    @Schema(example = "550e8400-e29b-41d4-a716-446655440000") UUID userId,
    @Schema(example = "carlos_ruiz") String username,
    @Schema(example = "MANAGER") UserRole role,
    @Schema(example = "TOKEN_EXPIRED") String error,
    @Schema(example = "The token has expired") String message,
    @Schema(example = "2026-10-05T10:30:00Z") Instant timestamp) {

  /** Builds the response for a valid token. */
  public static ValidateResponse success(UUID userId, String username, UserRole role) {
    return new ValidateResponse(true, userId, username, role, null, null, null);
  }

  /** Builds the response for an invalid or expired token. */
  public static ValidateResponse failure(String error, String message) {
    return new ValidateResponse(false, null, null, null, error, message, Instant.now());
  }
}
