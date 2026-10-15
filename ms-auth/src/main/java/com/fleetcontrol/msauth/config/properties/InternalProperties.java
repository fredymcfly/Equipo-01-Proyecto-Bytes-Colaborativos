package com.fleetcontrol.msauth.config.properties;

import com.fleetcontrol.msauth.constants.ConfigConstants;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Settings used to authenticate calls between services.
 *
 * @param apiKey shared key sent in the {@code X-Internal-Key} header
 */
@Validated
@ConfigurationProperties(prefix = ConfigConstants.Properties.FLEET_INTERNAL)
public record InternalProperties(@NotBlank String apiKey) {

  @Override
  public String toString() {
    return "InternalProperties[apiKey=***]";
  }
}
