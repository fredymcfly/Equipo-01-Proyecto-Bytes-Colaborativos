package com.fleetcontrol.msdrivers.config.properties;

import com.fleetcontrol.msdrivers.constants.ConfigConstants;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Settings of the demo data seeder. */
@ConfigurationProperties(prefix = ConfigConstants.Properties.FLEET_DEMO)
public record DemoProperties(Integer seed, Integer days) {}
