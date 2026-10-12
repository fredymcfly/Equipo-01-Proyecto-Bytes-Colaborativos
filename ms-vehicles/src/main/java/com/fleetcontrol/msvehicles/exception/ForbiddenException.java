package com.fleetcontrol.msvehicles.exception;

/** Thrown when the current user is not allowed to perform the operation. */
public class ForbiddenException extends ApiException {

  public ForbiddenException() {
    super(ErrorCode.FORBIDDEN);
  }
}
