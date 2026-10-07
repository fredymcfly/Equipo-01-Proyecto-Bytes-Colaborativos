package com.fleetcontrol.msfuel.exception;

import org.springframework.http.HttpStatus;

/** Códigos de error del contrato común con su estado HTTP y su mensaje por defecto. */
public enum ErrorCode {
  VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "La petición contiene campos no válidos"),
  VEHICLE_NOT_FOUND(HttpStatus.NOT_FOUND, "No existe un vehículo con el ID proporcionado"),
  INVALID_ODOMETER(
      HttpStatus.CONFLICT, "El odómetro es menor que el del último repostaje del vehículo"),
  SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "El servicio no está disponible");

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
