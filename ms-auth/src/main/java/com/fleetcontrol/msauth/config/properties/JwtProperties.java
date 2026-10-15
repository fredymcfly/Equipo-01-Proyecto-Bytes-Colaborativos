package com.fleetcontrol.msauth.config.properties;

import com.fleetcontrol.msauth.constants.ConfigConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Settings used to issue and validate JWT tokens (HS256).
 *
 * @param secret signing key, at least 32 characters
 * @param tokenPrefix token type returned to clients, for example {@code Bearer}
 * @param expirationMs token lifetime in milliseconds
 */
@Validated
@ConfigurationProperties(prefix = ConfigConstants.Properties.FLEET_JWT)
public record JwtProperties(
    @NotBlank @Size(min = 32, message = "JWT_SECRET must have at least 32 characters")
        String secret,
    @NotBlank String tokenPrefix,
    @Positive long expirationMs) {

  @Override
  public String toString() {
    return "JwtProperties[secret=***, tokenPrefix="
        + tokenPrefix
        + ", expirationMs="
        + expirationMs
        + "]";
  }
}
