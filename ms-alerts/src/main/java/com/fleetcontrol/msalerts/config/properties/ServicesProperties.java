package com.fleetcontrol.msalerts.config.properties;

import com.fleetcontrol.msalerts.constants.ConfigConstants;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Base URL of each source service. The Feign clients read the same keys through placeholders,
 * because Feign only accepts a placeholder in its {@code url}; this record declares them.
 */
@ConfigurationProperties(prefix = ConfigConstants.Properties.FLEET_SERVICES)
public record ServicesProperties(
    Service vehicles, Service drivers, Service maintenance, Service fuel) {

  /** Address of one service. */
  public record Service(String url) {}
}
