package com.fleetcontrol.msdashboard.config;

import com.fleetcontrol.msdashboard.constants.ResilienceConstants;
import feign.FeignException;
import feign.RetryableException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import java.util.concurrent.TimeoutException;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.cloud.openfeign.CircuitBreakerNameResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Circuit breaker settings shared by the Feign clients of the source services. */
@Configuration
public class ResilienceConfig {

  /**
   * Opens the circuit when 50 % of the last 10 calls fail and waits 30 seconds before trying again.
   * Only connection failures, timeouts and server errors count as failures, not 4xx answers.
   */
  @Bean
  public Customizer<Resilience4JCircuitBreakerFactory> circuitBreakerCustomizer() {
    return factory ->
        factory.configureDefault(
            id ->
                new Resilience4JConfigBuilder(id)
                    .circuitBreakerConfig(
                        CircuitBreakerConfig.custom()
                            .slidingWindowSize(ResilienceConstants.CircuitBreaker.WINDOW_SIZE)
                            .minimumNumberOfCalls(ResilienceConstants.CircuitBreaker.WINDOW_SIZE)
                            .failureRateThreshold(
                                ResilienceConstants.CircuitBreaker.FAILURE_RATE_PERCENT)
                            .waitDurationInOpenState(
                                ResilienceConstants.CircuitBreaker.OPEN_STATE_WAIT)
                            .recordException(ResilienceConfig::isServiceFailure)
                            .build())
                    .timeLimiterConfig(
                        TimeLimiterConfig.custom()
                            .timeoutDuration(ResilienceConstants.CircuitBreaker.CALL_TIMEOUT)
                            .build())
                    .build());
  }

  /** Gives every method of a client the same circuit, so there is one circuit per service. */
  @Bean
  public CircuitBreakerNameResolver circuitBreakerNameResolver() {
    return (feignClientName, target, method) -> feignClientName;
  }

  private static boolean isServiceFailure(Throwable throwable) {
    if (throwable instanceof RetryableException || throwable instanceof TimeoutException) {
      return true;
    }
    return throwable instanceof FeignException exception
        && exception.status() >= ResilienceConstants.CircuitBreaker.SERVER_ERROR_FROM;
  }
}
