package com.fleetcontrol.msgateway.config.properties;

import com.fleetcontrol.msgateway.constants.ConfigConstants;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Base URL of each microservice the gateway routes to. */
@ConfigurationProperties(prefix = ConfigConstants.Properties.FLEET_GATEWAY_SERVICES)
public record ServiceProperties(
    String auth,
    String routes,
    String dashboard,
    String vehicles,
    String alerts,
    String drivers,
    String fuel,
    String maintenance) {}
