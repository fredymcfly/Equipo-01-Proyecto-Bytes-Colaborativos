
package com.fleetcontrol.msdrivers.exception;
import com.fleetcontrol.msdrivers.exception.DriverNotFoundException;
public class DriverNotFoundException extends RuntimeException {

    public DriverNotFoundException() {
        super("DRIVER_NOT_FOUND");

    }

}
