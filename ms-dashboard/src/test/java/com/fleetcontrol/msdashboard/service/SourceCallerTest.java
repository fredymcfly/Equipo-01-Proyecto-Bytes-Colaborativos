package com.fleetcontrol.msdashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fleetcontrol.msdashboard.support.FeignFailures;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.circuitbreaker.NoFallbackAvailableException;

class SourceCallerTest {

  private static final String SERVICE = "ms-test";

  private final SourceCaller caller = new SourceCaller(Runnable::run);

  @Test
  void call_whenTheServiceAnswers_returnsItsData() {
    SourceResult<String> result = caller.<String>call(SERVICE, () -> "data").join();

    assertThat(result.isAvailable()).isTrue();
    assertThat(result.service()).isEqualTo(SERVICE);
    assertThat(result.data()).isEqualTo("data");
  }

  @Test
  void call_whenFeignFails_returnsAnUnavailableResultInsteadOfThrowing() {
    SourceResult<String> result =
        caller
            .<String>call(
                SERVICE,
                () -> {
                  throw FeignFailures.unavailable();
                })
            .join();

    assertThat(result.isAvailable()).isFalse();
    assertThat(result.service()).isEqualTo(SERVICE);
    assertThat(result.data()).isNull();
  }

  @Test
  void call_whenTheCircuitBreakerIsOpen_returnsAnUnavailableResultInsteadOfThrowing() {
    CallNotPermittedException open =
        CallNotPermittedException.createCallNotPermittedException(
            CircuitBreaker.ofDefaults(SERVICE));

    SourceResult<String> result =
        caller
            .<String>call(
                SERVICE,
                () -> {
                  throw open;
                })
            .join();

    assertThat(result.isAvailable()).isFalse();
    assertThat(result.service()).isEqualTo(SERVICE);
  }

  @Test
  void call_whenThereIsNoFallbackForTheFailure_returnsAnUnavailableResult() {
    SourceResult<String> result =
        caller
            .<String>call(
                SERVICE,
                () -> {
                  throw new NoFallbackAvailableException(
                      "No fallback available.", new IllegalStateException("circuit"));
                })
            .join();

    assertThat(result.isAvailable()).isFalse();
  }

  @Test
  void call_whenTheServiceAnswersNothing_isUnavailable() {
    SourceResult<String> result = caller.<String>call(SERVICE, () -> null).join();

    assertThat(result.isAvailable()).isFalse();
  }

  @Test
  void call_whenTheFailureIsNotFromTheCall_isNotHidden() {
    assertThatThrownBy(
            () ->
                caller
                    .<String>call(
                        SERVICE,
                        () -> {
                          throw new IllegalStateException("bug");
                        })
                    .join())
        .isInstanceOf(CompletionException.class)
        .hasCauseInstanceOf(IllegalStateException.class);
  }
}
