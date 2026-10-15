package com.fleetcontrol.msmaintenance.constants;

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
    public static final String VEHICLE_NOT_FOUND = "VEHICLE_NOT_FOUND";
    public static final String ORDER_NOT_FOUND = "ORDER_NOT_FOUND";
    public static final String PLAN_ALREADY_EXISTS = "PLAN_ALREADY_EXISTS";
    public static final String INVALID_ORDER_STATE = "INVALID_ORDER_STATE";
    public static final String VEHICLE_NOT_AVAILABLE = "VEHICLE_NOT_AVAILABLE";
    public static final String INVALID_ODOMETER = "INVALID_ODOMETER";
    public static final String SERVICE_UNAVAILABLE = "SERVICE_UNAVAILABLE";
  }

  /** Messages of the error responses. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Message {
    public static final String VALIDATION_ERROR = "The request contains invalid fields";
    public static final String UNAUTHORIZED = "Authentication is required";
    public static final String TOKEN_EXPIRED = "The token has expired";
    public static final String FORBIDDEN = "You do not have permission for this operation";
    public static final String VEHICLE_NOT_FOUND = "No vehicle exists with the provided id";
    public static final String ORDER_NOT_FOUND = "No maintenance order exists with the provided id";
    public static final String PLAN_ALREADY_EXISTS =
        "The vehicle already has an active plan of that maintenance type";
    public static final String INVALID_ORDER_STATE =
        "The order is not in a state that allows this action";
    public static final String VEHICLE_NOT_AVAILABLE =
        "The vehicle is not available for maintenance";
    public static final String INVALID_ODOMETER =
        "The odometer cannot be lower than the current one";
    public static final String SERVICE_UNAVAILABLE = "A required service is not responding";
  }

  /** Reasons of the field-level details. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Reason {
    public static final String REQUIRED = "is required";
    public static final String INVALID_VALUE = "has an invalid value";
  }
}
