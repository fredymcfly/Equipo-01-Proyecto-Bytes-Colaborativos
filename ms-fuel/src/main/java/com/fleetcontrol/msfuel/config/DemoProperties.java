package com.fleetcontrol.msfuel.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Ajustes del seeder de datos demo.
 *
 * @param seed semilla del generador aleatorio; la misma semilla produce los mismos repostajes
 * @param days días de historial que se generan
 */
@ConfigurationProperties(prefix = "fleet.demo")
public record DemoProperties(Integer seed, Integer days) {}
