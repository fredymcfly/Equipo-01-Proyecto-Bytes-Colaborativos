package com.fleetcontrol.msvehicles.exception;

/** Thrown when a vehicle with the same plate already exists. */
public class VehicleAlreadyExistsException extends ApiException {

  public VehicleAlreadyExistsException() {
    super(ErrorCode.VEHICLE_ALREADY_EXISTS);
  }
}
