package com.fleetcontrol.msmaintenance.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Enables the scheduled tasks, unless {@code fleet.scheduling.enabled} is set to false. */
@Configuration
@EnableScheduling
@ConditionalOnProperty(
    name = "fleet.scheduling.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class SchedulingConfig {}
