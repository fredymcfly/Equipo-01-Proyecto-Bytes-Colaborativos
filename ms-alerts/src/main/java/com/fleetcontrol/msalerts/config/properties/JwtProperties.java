package com.fleetcontrol.msalerts.config.properties;

import com.fleetcontrol.msalerts.constants.ConfigConstants;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Settings used to validate the JWT of incoming requests. */
@ConfigurationProperties(prefix = ConfigConstants.Properties.FLEET_JWT)
public record JwtProperties(String secret, String tokenPrefix) {}
