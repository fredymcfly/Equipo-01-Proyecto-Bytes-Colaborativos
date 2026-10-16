package com.fleetcontrol.msmaintenance.config.properties;

import com.fleetcontrol.msmaintenance.constants.ConfigConstants;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Settings of the demo data seeder. */
@ConfigurationProperties(prefix = ConfigConstants.Properties.FLEET_DEMO)
public record DemoProperties(Integer seed, Integer days) {}
