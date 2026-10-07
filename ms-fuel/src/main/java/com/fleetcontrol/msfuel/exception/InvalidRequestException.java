package com.fleetcontrol.msfuel.exception;

/** La petición incumple una regla de negocio y no puede procesarse tal y como llega. */
public class InvalidRequestException extends ApiException {
  public InvalidRequestException(String message) {
    super(ErrorCode.VALIDATION_ERROR, message);
  }
}
