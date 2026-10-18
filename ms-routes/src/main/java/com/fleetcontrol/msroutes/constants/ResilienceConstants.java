package com.fleetcontrol.msroutes.constants;

import java.time.Duration;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Resilience settings of the calls to ms-vehicles and ms-drivers. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ResilienceConstants {

  /** Circuit breaker of each client. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class CircuitBreaker {
    public static final int WINDOW_SIZE = 10;
    public static final float FAILURE_RATE_PERCENT = 50;
    public static final Duration OPEN_STATE_WAIT = Duration.ofSeconds(30);

    /** Covers two attempts of 2 s to connect and 5 s to read, plus the pause between them. */
    public static final Duration CALL_TIMEOUT = Duration.ofSeconds(16);

    /** Status from which an answer counts as a failure of the service. */
    public static final int SERVER_ERROR_FROM = 500;
  }

  /** Retries of the calls. Only the GET calls are repeated. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Retry {
    /** One retry after the first attempt. */
    public static final int MAX_ATTEMPTS = 2;

    public static final long PERIOD_MS = 100;
    public static final long MAX_PERIOD_MS = 1000;
  }
}
