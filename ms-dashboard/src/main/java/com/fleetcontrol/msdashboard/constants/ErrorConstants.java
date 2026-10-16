package com.fleetcontrol.msdashboard.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Utility class holding the codes and messages of the error responses. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ErrorConstants {

  /** Error codes defined by the shared error contract. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Code {
    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String UNAUTHORIZED = "UNAUTHORIZED";
    public static final String TOKEN_EXPIRED = "TOKEN_EXPIRED";
    public static final String FORBIDDEN = "FORBIDDEN";
    public static final String SERVICE_UNAVAILABLE = "SERVICE_UNAVAILABLE";
  }

  /** Messages of the error responses. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Message {
    public static final String VALIDATION_ERROR = "The request contains invalid fields";
    public static final String UNAUTHORIZED = "Authentication is required";
    public static final String TOKEN_EXPIRED = "The token has expired";
    public static final String FORBIDDEN = "You do not have permission for this operation";
    public static final String SERVICE_UNAVAILABLE =
        "A service needed to answer the request is not available";
  }

  /** Reasons of the field-level details. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Reason {
    public static final String INVALID_VALUE = "has an invalid value";
    public static final String REQUIRED = "is required";
    public static final String METRICS_REQUIRED = "at least one metric is required";
    public static final String TO_BEFORE_FROM = "must not be before from";
    public static final String RANGE_TOO_LARGE = "the range must not exceed 366 days";
  }
}
