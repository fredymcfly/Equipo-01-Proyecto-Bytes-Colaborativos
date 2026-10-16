package com.fleetcontrol.msdashboard.config.properties;

import com.fleetcontrol.msdashboard.constants.ConfigConstants;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Base URL of each source service. The Feign clients read the same keys through placeholders,
 * because Feign only accepts a placeholder in its {@code url}; this record declares them.
 */
@ConfigurationProperties(prefix = ConfigConstants.Properties.FLEET_SERVICES)
public record ServicesProperties(
    Service vehicles, Service routes, Service fuel, Service maintenance, Service alerts) {

  /** Address of one service. */
  public record Service(String url) {}
}
