package com.fleetcontrol.msfuel.config;

import feign.Retryer;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Configuracion de los clientes Feign de ms-fuel. */
@Configuration
@EnableFeignClients(basePackages = "com.fleetcontrol.msfuel.client")
public class FeignConfig {

  private static final long RETRY_PERIOD_MILLIS = 100L;
  private static final int MAX_ATTEMPTS = 2;

  /**
   * Un unico reintento por llamada, esperando 100 ms entre intentos.
   *
   * <p>Los timeouts de conexion y de lectura van en properties, aqui solo la politica de reintento.
   * El circuit breaker tambien la respeta: con el starter de resilience4j presente, {@code
   * CircuitBreakerPresentFeignBuilderConfiguration} inyecta este mismo bean en el builder.
   *
   * <p>Ojo: Feign no distingue metodos HTTP, asi que el reintento alcanza a todos. Hoy el unico
   * cliente solo expone GET. Cuando se le anadan POST o PATCH, ese cliente necesita su propia
   * configuracion con {@code Retryer.NEVER_RETRY}, que tiene prioridad sobre este bean.
   */
  @Bean
  public Retryer retryer() {
    return new Retryer.Default(RETRY_PERIOD_MILLIS, RETRY_PERIOD_MILLIS, MAX_ATTEMPTS);
  }
}
