package com.fleetcontrol.msmaintenance.config;

import com.fleetcontrol.msmaintenance.constants.ClientConstants;
import com.fleetcontrol.msmaintenance.exception.ServiceUnavailableException;
import feign.RetryableException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Circuit breaker settings shared by every Feign client. */
@Configuration
public class ResilienceConfig {

  /**
   * Opens the circuit when 50 % of the last 10 calls fail and waits 30 seconds before trying again.
   * Only connection failures and server errors count as failures, not 4xx answers.
   */
  @Bean
  public Customizer<Resilience4JCircuitBreakerFactory> circuitBreakerCustomizer() {
    return factory ->
        factory.configureDefault(
            id ->
                new Resilience4JConfigBuilder(id)
                    .circuitBreakerConfig(
                        CircuitBreakerConfig.custom()
                            .slidingWindowSize(ClientConstants.CircuitBreaker.WINDOW_SIZE)
                            .minimumNumberOfCalls(ClientConstants.CircuitBreaker.WINDOW_SIZE)
                            .failureRateThreshold(
                                ClientConstants.CircuitBreaker.FAILURE_RATE_PERCENT)
                            .waitDurationInOpenState(ClientConstants.CircuitBreaker.OPEN_STATE_WAIT)
                            .recordExceptions(
                                ServiceUnavailableException.class, RetryableException.class)
                            .build())
                    .timeLimiterConfig(
                        TimeLimiterConfig.custom()
                            .timeoutDuration(ClientConstants.CircuitBreaker.CALL_TIMEOUT)
                            .build())
                    .build());
  }
}
