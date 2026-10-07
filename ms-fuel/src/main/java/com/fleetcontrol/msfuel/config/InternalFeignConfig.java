package com.fleetcontrol.msfuel.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Añade la clave interna a las llamadas que ms-fuel hace a otros microservicios. */
@Configuration
public class InternalFeignConfig {

  /** Intercepta la llamada saliente para firmarla con X-Internal-Key. */
  @Bean
  public RequestInterceptor internalKeyInterceptor(@Value("${internal.api-key}") String apiKey) {
    return template -> template.header("X-Internal-Key", apiKey);
  }
}
