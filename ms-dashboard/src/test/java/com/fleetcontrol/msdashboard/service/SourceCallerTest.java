package com.fleetcontrol.msdashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fleetcontrol.msdashboard.support.FeignFailures;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.Test;

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
