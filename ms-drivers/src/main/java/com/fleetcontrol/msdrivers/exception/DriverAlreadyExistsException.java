package com.fleetcontrol.msdrivers.exception;

import com.fleetcontrol.msdrivers.constants.ErrorConstants;
import java.io.Serial;
import org.springframework.http.HttpStatus;

/** Thrown when a driver with the same license number already exists. */
public class DriverAlreadyExistsException extends ApiException {

  @Serial private static final long serialVersionUID = 1L;

  /** Creates the exception with the default message. */
  public DriverAlreadyExistsException() {
    super(
        HttpStatus.CONFLICT,
        ErrorConstants.Code.DRIVER_ALREADY_EXISTS,
        ErrorConstants.Message.DRIVER_ALREADY_EXISTS);
  }
}
