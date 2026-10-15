package com.fleetcontrol.msmaintenance.exception;

import com.fleetcontrol.msmaintenance.constants.ErrorConstants;

/** Thrown when the submitted odometer is lower than the current one. */
public class InvalidOdometerException extends ApiException {

  private static final long serialVersionUID = 1L;

  /** Creates the exception with the default message. */
  public InvalidOdometerException() {
    super(ErrorConstants.Code.INVALID_ODOMETER, ErrorConstants.Message.INVALID_ODOMETER);
  }
}
