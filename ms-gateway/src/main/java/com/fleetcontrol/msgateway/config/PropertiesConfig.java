package com.fleetcontrol.msgateway.config;

import com.fleetcontrol.msgateway.constants.ConfigConstants;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Configuration;

/** Enables the scanning of the classes annotated with {@code @ConfigurationProperties}. */
@Configuration
@ConfigurationPropertiesScan(basePackages = ConfigConstants.BASE_PACKAGE)
public class PropertiesConfig {}
