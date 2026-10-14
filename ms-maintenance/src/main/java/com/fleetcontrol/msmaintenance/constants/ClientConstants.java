package com.fleetcontrol.msmaintenance.constants;

import java.time.Duration;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Utility class holding the settings of the calls to other services. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ClientConstants {

  /** HTTP statuses that the error decoder tells apart. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Http {
    public static final int NOT_FOUND = 404;
    public static final int CONFLICT = 409;
  }

  /** Error codes returned by ms-vehicles. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class VehicleErrors {
    public static final String INVALID_ODOMETER = "INVALID_ODOMETER";
    public static final String INVALID_STATUS_TRANSITION = "INVALID_STATUS_TRANSITION";
  }

  /** Attempts per call: reads are retried once, writes are not. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Attempts {
    public static final int READ = 2;
    public static final int WRITE = 1;
  }

  /** Circuit breaker settings. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class CircuitBreaker {
    public static final int WINDOW_SIZE = 10;
    public static final float FAILURE_RATE_PERCENT = 50;
    public static final Duration OPEN_STATE_WAIT = Duration.ofSeconds(30);
    public static final Duration CALL_TIMEOUT = Duration.ofSeconds(12);
  }
}
