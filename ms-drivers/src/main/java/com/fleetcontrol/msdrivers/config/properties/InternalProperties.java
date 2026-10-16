package com.fleetcontrol.msdrivers.config.properties;

import com.fleetcontrol.msdrivers.constants.ConfigConstants;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Key shared by the services for internal calls. */
@ConfigurationProperties(prefix = ConfigConstants.Properties.FLEET_INTERNAL)
public record InternalProperties(String apiKey) {}
