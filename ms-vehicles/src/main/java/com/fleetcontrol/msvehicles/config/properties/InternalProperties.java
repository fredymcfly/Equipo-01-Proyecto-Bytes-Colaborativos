package com.fleetcontrol.msvehicles.config.properties;

import com.fleetcontrol.msvehicles.constants.ConfigConstants;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Key shared by the services for internal calls. */
@ConfigurationProperties(prefix = ConfigConstants.Properties.FLEET_INTERNAL)
public record InternalProperties(String apiKey) {}
