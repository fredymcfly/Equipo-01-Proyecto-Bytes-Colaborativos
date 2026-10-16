package com.fleetcontrol.msvehicles.exception;

import com.fleetcontrol.msvehicles.constants.ErrorConstants;
import java.io.Serial;
import org.springframework.http.HttpStatus;

/** Thrown when a vehicle with the same plate already exists. */
public class VehicleAlreadyExistsException extends ApiException {

  @Serial private static final long serialVersionUID = 1L;

  /** Creates the exception with the default message. */
  public VehicleAlreadyExistsException() {
    super(
        HttpStatus.CONFLICT,
        ErrorConstants.Code.VEHICLE_ALREADY_EXISTS,
        ErrorConstants.Message.VEHICLE_ALREADY_EXISTS);
  }
}
