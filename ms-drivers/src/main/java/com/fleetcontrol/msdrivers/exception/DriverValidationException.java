
package com.fleetcontrol.msdrivers.exception;

public class DriverValidationException extends RuntimeException {

    public DriverValidationException() {
        super("VALIDATION_ERROR");
    }
}
