package com.fleetcontrol.msauth.models.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.Locale;

/** Body of {@code POST /api/auth/login}. The email is normalized to lowercase. */
public record LoginRequest(
    @Schema(description = "Email address of the user", example = "carlos@fleetcontrol.com")
        @NotBlank(message = "is required")
        String email,
    @Schema(description = "Password of the user", example = "FleetPass123!")
        @NotBlank(message = "is required")
        String password) {

  /** Trims and lowercases the email so it matches the stored value. */
  public LoginRequest {
    if (email != null) {
      email = email.trim().toLowerCase(Locale.ROOT);
    }
  }

  @Override
  public String toString() {
    return "LoginRequest[email=" + email + ", password=***]";
  }
}
