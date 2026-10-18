package com.fleetcontrol.msroutes.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import feign.Request;
import feign.RetryableException;
import feign.Retryer;
import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Unit tests of the retry policy: one retry, and only for GET calls. */
class GetOnlyRetryerTest {

  private static final long PERIOD_MS = 1;
  private static final int MAX_ATTEMPTS = 2;

  @Test
  void continueOrPropagate_getCall_isRetriedOnce() {
    Retryer retryer = new GetOnlyRetryer(PERIOD_MS, PERIOD_MS, MAX_ATTEMPTS);
    RetryableException failure = failure(Request.HttpMethod.GET);

    assertThatCode(() -> retryer.continueOrPropagate(failure)).doesNotThrowAnyException();
    assertThatThrownBy(() -> retryer.continueOrPropagate(failure)).isSameAs(failure);
  }

  @Test
  void continueOrPropagate_patchCall_isNeverRetried() {
    Retryer retryer = new GetOnlyRetryer(PERIOD_MS, PERIOD_MS, MAX_ATTEMPTS);
    RetryableException failure = failure(Request.HttpMethod.PATCH);

    assertThatThrownBy(() -> retryer.continueOrPropagate(failure)).isSameAs(failure);
  }

  @Test
  void continueOrPropagate_postCall_isNeverRetried() {
    Retryer retryer = new GetOnlyRetryer(PERIOD_MS, PERIOD_MS, MAX_ATTEMPTS);
    RetryableException failure = failure(Request.HttpMethod.POST);

    assertThatThrownBy(() -> retryer.continueOrPropagate(failure)).isSameAs(failure);
  }

  @Test
  void clone_startsWithoutAttempts() {
    Retryer retryer = new GetOnlyRetryer(PERIOD_MS, PERIOD_MS, MAX_ATTEMPTS);
    RetryableException failure = failure(Request.HttpMethod.GET);
    retryer.continueOrPropagate(failure);

    Retryer fresh = retryer.clone();

    assertThatCode(() -> fresh.continueOrPropagate(failure)).doesNotThrowAnyException();
  }

  private static RetryableException failure(Request.HttpMethod method) {
    Request request =
        Request.create(
            method,
            "http://ms-vehicles/api",
            Map.of(),
            (byte[]) null,
            StandardCharsets.UTF_8,
            null);
    return new RetryableException(
        -1, "Connection refused", method, new ConnectException("refused"), (Long) null, request);
  }
}
