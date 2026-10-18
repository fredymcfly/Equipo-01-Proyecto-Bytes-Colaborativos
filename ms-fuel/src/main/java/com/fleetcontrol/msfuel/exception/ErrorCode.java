package com.fleetcontrol.msfuel.exception;

import org.springframework.http.HttpStatus;

/** Códigos de error del contrato común con su estado HTTP y su mensaje por defecto. */
public enum ErrorCode {
  VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "The request contains invalid values"),
  VEHICLE_NOT_FOUND(HttpStatus.NOT_FOUND, "No vehicle was found with the provided id."),
  INVALID_ODOMETER(
      HttpStatus.CONFLICT, "The odometer is lower than the last refuel of the vehicle"),
  SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "The service is unavailable"),
  UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "A valid authentication token is required"),
  TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "The token has expired"),
  FORBIDDEN(HttpStatus.FORBIDDEN, "The user's role does not have permission for this operation");

  private final HttpStatus status;
  private final String defaultMessage;

  ErrorCode(HttpStatus status, String defaultMessage) {
    this.status = status;
    this.defaultMessage = defaultMessage;
  }

  /** Estado HTTP con el que se devuelve este código. */
  public HttpStatus getStatus() {
    return status;
  }

  /** Mensaje que se devuelve cuando la petición no aporta uno más concreto. */
  public String getDefaultMessage() {
    return defaultMessage;
  }
}
