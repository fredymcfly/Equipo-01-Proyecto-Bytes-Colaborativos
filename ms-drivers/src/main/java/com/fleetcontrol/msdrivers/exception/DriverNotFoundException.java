package com.fleetcontrol.msdrivers.exception;

import com.fleetcontrol.msdrivers.constants.ErrorConstants;
import java.io.Serial;
import org.springframework.http.HttpStatus;

/** Thrown when a driver does not exist. */
public class DriverNotFoundException extends ApiException {

  @Serial private static final long serialVersionUID = 1L;

  /** Creates the exception with the default message. */
  public DriverNotFoundException() {
    super(
        HttpStatus.NOT_FOUND,
        ErrorConstants.Code.DRIVER_NOT_FOUND,
        ErrorConstants.Message.DRIVER_NOT_FOUND);
  }
}
