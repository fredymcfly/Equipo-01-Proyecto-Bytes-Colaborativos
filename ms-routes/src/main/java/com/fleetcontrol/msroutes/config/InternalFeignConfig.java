package com.fleetcontrol.msroutes.config;

import com.fleetcontrol.msroutes.constants.ResilienceConstants;
import feign.RequestInterceptor;
import feign.Retryer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

/** Adds the internal API key to every Feign call. */
public class InternalFeignConfig {

  @Bean
  public RequestInterceptor internalKeyInterceptor(@Value("${internal.api-key}") String apiKey) {
    return template -> template.header("X-Internal-Key", apiKey);
  }

  /** One retry, only for GET calls that could not reach the service. */
  @Bean
  public Retryer retryer() {
    return new GetOnlyRetryer(
        ResilienceConstants.Retry.PERIOD_MS,
        ResilienceConstants.Retry.MAX_PERIOD_MS,
        ResilienceConstants.Retry.MAX_ATTEMPTS);
  }
}
