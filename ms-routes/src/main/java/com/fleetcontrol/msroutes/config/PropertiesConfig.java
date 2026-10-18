package com.fleetcontrol.msroutes.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Registers the configuration properties classes. */
@Configuration
@EnableConfigurationProperties(DemoProperties.class)
public class PropertiesConfig {}
