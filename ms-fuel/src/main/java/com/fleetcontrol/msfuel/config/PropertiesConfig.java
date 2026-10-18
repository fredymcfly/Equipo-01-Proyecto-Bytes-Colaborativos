package com.fleetcontrol.msfuel.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Registra las clases de propiedades de configuración. */
@Configuration
@EnableConfigurationProperties(DemoProperties.class)
public class PropertiesConfig {}
