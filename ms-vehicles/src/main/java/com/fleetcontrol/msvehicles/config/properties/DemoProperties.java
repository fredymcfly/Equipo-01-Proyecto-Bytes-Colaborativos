package com.fleetcontrol.msvehicles.config.properties;

import com.fleetcontrol.msvehicles.constants.ConfigConstants;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Settings of the demo data seeder. */
@ConfigurationProperties(prefix = ConfigConstants.Properties.FLEET_DEMO)
public record DemoProperties(Integer seed, Integer days) {}
