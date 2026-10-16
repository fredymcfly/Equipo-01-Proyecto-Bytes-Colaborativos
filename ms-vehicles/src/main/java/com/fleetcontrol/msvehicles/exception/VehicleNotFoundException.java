package com.fleetcontrol.msvehicles.exception;

import com.fleetcontrol.msvehicles.constants.ErrorConstants;
import java.io.Serial;
import org.springframework.http.HttpStatus;

/** Thrown when a vehicle does not exist. */
public class VehicleNotFoundException extends ApiException {

  @Serial private static final long serialVersionUID = 1L;

  /** Creates the exception with the default message. */
  public VehicleNotFoundException() {
    super(
        HttpStatus.NOT_FOUND,
        ErrorConstants.Code.VEHICLE_NOT_FOUND,
        ErrorConstants.Message.VEHICLE_NOT_FOUND);
  }
}
