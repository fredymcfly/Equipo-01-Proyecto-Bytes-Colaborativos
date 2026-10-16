package com.fleetcontrol.msmaintenance.exception;

import com.fleetcontrol.msmaintenance.constants.ErrorConstants;

/** Thrown when ms-vehicles does not know the vehicle. */
public class VehicleNotFoundException extends ApiException {

  private static final long serialVersionUID = 1L;

  /** Creates the exception with the default message. */
  public VehicleNotFoundException() {
    super(ErrorConstants.Code.VEHICLE_NOT_FOUND, ErrorConstants.Message.VEHICLE_NOT_FOUND);
  }
}
