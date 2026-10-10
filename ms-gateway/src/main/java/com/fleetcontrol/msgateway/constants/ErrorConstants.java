package com.fleetcontrol.msgateway.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Error codes and messages the gateway returns, following the common error contract. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ErrorConstants {
  public static final String SERVICE_UNAVAILABLE = "SERVICE_UNAVAILABLE";
  public static final String SERVICE_UNAVAILABLE_MESSAGE = "Service %s is not responding";
  public static final String NOT_FOUND = "NOT_FOUND";
  public static final String NOT_FOUND_MESSAGE = "Resource not found";
  public static final String INTERNAL_ERROR = "INTERNAL_ERROR";
  public static final String INTERNAL_ERROR_MESSAGE = "Unexpected error in the gateway";
  public static final String RATE_LIMIT_EXCEEDED = "RATE_LIMIT_EXCEEDED";
  public static final String RATE_LIMIT_EXCEEDED_MESSAGE = "Too many requests. Limit: %d req/min";
}
