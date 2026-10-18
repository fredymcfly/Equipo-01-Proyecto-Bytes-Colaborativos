package com.fleetcontrol.msdashboard.client;

import com.fleetcontrol.msdashboard.config.properties.InternalProperties;
import com.fleetcontrol.msdashboard.constants.ResilienceConstants;
import com.fleetcontrol.msdashboard.constants.SecurityConstants;
import feign.RequestInterceptor;
import feign.Retryer;
import org.springframework.context.annotation.Bean;

/**
 * Settings shared by the clients of the source services. It is not annotated with
 * {@code @Configuration} on purpose, so it only applies to the clients that reference it.
 */
public class InternalFeignConfig {

  /** Adds the internal key to every call. */
  @Bean
  public RequestInterceptor internalKeyInterceptor(InternalProperties properties) {
    return template -> template.header(SecurityConstants.INTERNAL_KEY_HEADER, properties.apiKey());
  }

  /** Retries once a call that could not reach the service. Every call of the clients is a GET. */
  @Bean
  public Retryer retryer() {
    return new Retryer.Default(
        ResilienceConstants.Retry.PERIOD_MS,
        ResilienceConstants.Retry.MAX_PERIOD_MS,
        ResilienceConstants.Retry.MAX_ATTEMPTS);
  }
}
