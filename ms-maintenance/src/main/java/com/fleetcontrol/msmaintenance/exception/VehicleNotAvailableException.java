package com.fleetcontrol.msmaintenance.exception;

import com.fleetcontrol.msmaintenance.constants.ErrorConstants;

/** Thrown when the vehicle cannot enter maintenance, for example because it is in use. */
public class VehicleNotAvailableException extends ApiException {

  private static final long serialVersionUID = 1L;

  /** Creates the exception with the default message. */
  public VehicleNotAvailableException() {
    super(ErrorConstants.Code.VEHICLE_NOT_AVAILABLE, ErrorConstants.Message.VEHICLE_NOT_AVAILABLE);
  }
}
