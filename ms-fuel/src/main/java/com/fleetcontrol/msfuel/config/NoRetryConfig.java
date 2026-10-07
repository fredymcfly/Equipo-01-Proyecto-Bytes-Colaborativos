package com.fleetcontrol.msfuel.config;

import feign.Retryer;
import org.springframework.context.annotation.Bean;

/**
 * Desactiva el reintento global para clientes con operaciones no idempotentes.
 *
 * <p>Feign no distingue metodos HTTP, asi que reintenta tambien los POST. Esta configuracion tiene
 * prioridad sobre el bean global de {@link FeignConfig}.
 */
public class NoRetryConfig {
  @Bean
  public Retryer retryer() {
    return Retryer.NEVER_RETRY;
  }
}
