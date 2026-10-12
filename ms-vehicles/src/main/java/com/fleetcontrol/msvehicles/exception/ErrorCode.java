package com.fleetcontrol.msvehicles.exception;

import org.springframework.http.HttpStatus;

/** Error codes defined by the shared error contract. */
public enum ErrorCode {
  VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "The request contains invalid fields"),
  UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Authentication is required"),
  TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "The token has expired"),
  FORBIDDEN(HttpStatus.FORBIDDEN, "You do not have permission for this operation"),
  VEHICLE_NOT_FOUND(HttpStatus.NOT_FOUND, "No vehicle exists with the provided id"),
  VEHICLE_ALREADY_EXISTS(HttpStatus.CONFLICT, "A vehicle with that plate already exists"),
  INVALID_STATUS_TRANSITION(HttpStatus.CONFLICT, "The status transition is not allowed"),
  INVALID_ODOMETER(HttpStatus.CONFLICT, "The odometer cannot be lower than the current one");

  private final HttpStatus status;
  private final String defaultMessage;

  ErrorCode(HttpStatus status, String defaultMessage) {
    this.status = status;
    this.defaultMessage = defaultMessage;
  }

  /** HTTP status that corresponds to this error code. */
  public HttpStatus status() {
    return status;
  }

  /** Human readable message used when no specific one is given. */
  public String defaultMessage() {
    return defaultMessage;
  }
}
