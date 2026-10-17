package com.fleetcontrol.msalerts.constants;

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
    public static final String ALERT_NOT_FOUND = "ALERT_NOT_FOUND";
    public static final String RULE_NOT_FOUND = "RULE_NOT_FOUND";
    public static final String INVALID_ALERT_STATE = "INVALID_ALERT_STATE";
  }

  /** Messages of the error responses. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Message {
    public static final String VALIDATION_ERROR = "The request contains invalid fields";
    public static final String UNAUTHORIZED = "Authentication is required";
    public static final String TOKEN_EXPIRED = "The token has expired";
    public static final String FORBIDDEN = "You do not have permission for this operation";
    public static final String ALERT_NOT_FOUND = "Alert not found";
    public static final String RULE_NOT_FOUND = "Alert rule not found";
    public static final String INVALID_ALERT_STATE = "Only an open alert can be acknowledged";
  }

  /** Reasons of the field-level details. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Reason {
    public static final String INVALID_VALUE = "has an invalid value";
    public static final String REQUIRED = "is required";
    public static final String TO_BEFORE_FROM = "must not be before from";
    public static final String RANGE_TOO_LARGE = "the range must not exceed 366 days";
    public static final String UNKNOWN_PARAM = "is not a parameter of this rule";
    public static final String NOT_INTEGER = "must be an integer";
    public static final String OUT_OF_RANGE = "must be between %d and %d";
    public static final String NO_PARAMS_ALLOWED = "this rule has no parameters";
  }
}
