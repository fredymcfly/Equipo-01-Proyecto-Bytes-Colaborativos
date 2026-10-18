package com.fleetcontrol.msalerts.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Provides the clock used for every date calculation, always in UTC. */
@Configuration
public class ClockConfig {

  /** Clock fixed to UTC, which tests can replace to control the current time. */
  @Bean
  public Clock clock() {
    return Clock.systemUTC();
  }
}
