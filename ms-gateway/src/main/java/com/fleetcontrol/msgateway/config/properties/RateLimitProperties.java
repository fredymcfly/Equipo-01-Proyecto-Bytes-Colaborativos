package com.fleetcontrol.msgateway.config.properties;

import com.fleetcontrol.msgateway.constants.ConfigConstants;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Rate limiting settings.
 *
 * @param requestsPerMinute maximum requests a single client IP can make in one minute
 */
@ConfigurationProperties(prefix = ConfigConstants.Properties.FLEET_RATE_LIMIT)
public record RateLimitProperties(@DefaultValue("60") int requestsPerMinute) {

  /** Rejects limits below one request per minute. */
  public RateLimitProperties {
    if (requestsPerMinute < 1) {
      throw new IllegalArgumentException("requests-per-minute must be at least 1");
    }
  }
}
