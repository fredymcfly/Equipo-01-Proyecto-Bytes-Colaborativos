package com.fleetcontrol.msfuel.exception;

public class InvalidRequestException extends ApiException {
    public InvalidRequestException(String message) {
        super(ErrorCode.VALIDATION_ERROR, message);
    }
}
