package com.fleetcontrol.msauth.config;

import com.fleetcontrol.msauth.constants.ConfigConstants;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Configuration;

/** Enables the scanning of classes annotated with {@code @ConfigurationProperties}. */
@Configuration
@ConfigurationPropertiesScan(basePackages = ConfigConstants.BASE_PACKAGE)
public class PropertiesConfig {}
