package com.fleetcontrol.msvehicles.exception;

/** Thrown when the submitted odometer is lower than the current one. */
public class InvalidOdometerException extends ApiException {

  public InvalidOdometerException() {
    super(ErrorCode.INVALID_ODOMETER);
  }
}
