package com.fleetcontrol.msvehicles.exception;

import com.fleetcontrol.msvehicles.constants.ErrorConstants;
import java.io.Serial;
import org.springframework.http.HttpStatus;

/** Thrown when a status change is not allowed by the rules of the fleet. */
public class InvalidStatusTransitionException extends ApiException {

  @Serial private static final long serialVersionUID = 1L;

  /** Creates the exception with the default message. */
  public InvalidStatusTransitionException() {
    super(
        HttpStatus.CONFLICT,
        ErrorConstants.Code.INVALID_STATUS_TRANSITION,
        ErrorConstants.Message.INVALID_STATUS_TRANSITION);
  }
}
