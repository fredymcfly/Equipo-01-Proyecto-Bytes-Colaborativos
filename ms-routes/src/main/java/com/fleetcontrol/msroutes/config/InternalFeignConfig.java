package com.fleetcontrol.msroutes.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

/** Adds the internal API key to every Feign call. */
public class InternalFeignConfig {

  @Bean
  public RequestInterceptor internalKeyInterceptor(@Value("${internal.api-key}") String apiKey) {
    return template -> template.header("X-Internal-Key", apiKey);
  }
}
