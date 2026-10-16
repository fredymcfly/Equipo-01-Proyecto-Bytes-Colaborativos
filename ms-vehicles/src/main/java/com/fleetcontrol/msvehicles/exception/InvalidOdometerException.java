package com.fleetcontrol.msvehicles.exception;

import com.fleetcontrol.msvehicles.constants.ErrorConstants;
import java.io.Serial;
import org.springframework.http.HttpStatus;

/** Thrown when the new odometer is lower than the current one. */
public class InvalidOdometerException extends ApiException {

  @Serial private static final long serialVersionUID = 1L;

  /** Creates the exception with the default message. */
  public InvalidOdometerException() {
    super(
        HttpStatus.CONFLICT,
        ErrorConstants.Code.INVALID_ODOMETER,
        ErrorConstants.Message.INVALID_ODOMETER);
  }
}
