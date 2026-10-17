package com.fleetcontrol.msalerts.config.properties;

import com.fleetcontrol.msalerts.constants.ConfigConstants;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Key shared by the services for internal calls. */
@ConfigurationProperties(prefix = ConfigConstants.Properties.FLEET_INTERNAL)
public record InternalProperties(String apiKey) {}
