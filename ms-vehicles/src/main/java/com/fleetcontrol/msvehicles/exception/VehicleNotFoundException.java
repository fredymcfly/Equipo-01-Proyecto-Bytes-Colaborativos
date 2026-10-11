package com.fleetcontrol.msvehicles.exception;

/** Thrown when a vehicle does not exist. */
public class VehicleNotFoundException extends ApiException {

  public VehicleNotFoundException() {
    super(ErrorCode.VEHICLE_NOT_FOUND);
  }
}
