package com.fleetcontrol.msroutes.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings of the demo data seeder.
 *
 * @param seed seed of the random generator; the same seed produces the same routes
 * @param days how many days of history are generated
 */
@ConfigurationProperties(prefix = "fleet.demo")
public record DemoProperties(Integer seed, Integer days) {}
