package com.fleetcontrol.msvehicles.exception;

/** Thrown when a vehicle status transition is not allowed. */
public class InvalidStatusTransitionException extends ApiException {

  public InvalidStatusTransitionException() {
    super(ErrorCode.INVALID_STATUS_TRANSITION);
  }
}
