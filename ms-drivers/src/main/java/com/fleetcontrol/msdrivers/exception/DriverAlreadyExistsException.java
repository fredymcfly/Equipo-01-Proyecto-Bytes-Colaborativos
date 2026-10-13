package com.fleetcontrol.msdrivers.exception;

public class DriverAlreadyExistsException extends RuntimeException {

    public DriverAlreadyExistsException() {
        super("DRIVER_ALREADY_EXISTS");
    }
}
