package com.fleetcontrol.msauth.models.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Locale;

/** Body of {@code POST /api/auth/register}. The email is normalized to lowercase. */
public record RegisterRequest(
    @Schema(description = "Unique username, without spaces", example = "carlos_ruiz")
        @NotBlank(message = "is required")
        @Size(max = 50, message = "must not exceed 50 characters")
        @Pattern(regexp = "^\\S*$", message = "must not contain spaces")
        String username,
    @Schema(description = "Unique email address", example = "carlos@fleetcontrol.com")
        @NotBlank(message = "is required")
        @Email(message = "must be a valid email address")
        @Size(max = 255, message = "must not exceed 255 characters")
        String email,
    @Schema(
            description = "At least 8 characters, with one uppercase letter and one number",
            example = "FleetPass123!")
        @NotBlank(message = "is required")
        @Size(min = 8, max = 72, message = "must be between 8 and 72 characters long")
        @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*\\d).*$",
            message = "must contain at least one uppercase letter and one number")
        String password) {

  /** Trims and lowercases the email so validation and persistence see the same value. */
  public RegisterRequest {
    if (email != null) {
      email = email.trim().toLowerCase(Locale.ROOT);
    }
  }

  @Override
  public String toString() {
    return "RegisterRequest[username=" + username + ", email=" + email + ", password=***]";
  }
}
