package com.fleetcontrol.msfuel.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Reloj de la aplicación, sustituible en los tests. */
@Configuration
public class ClockConfig {

  @Bean
  Clock clock() {
    return Clock.systemUTC();
  }
}
