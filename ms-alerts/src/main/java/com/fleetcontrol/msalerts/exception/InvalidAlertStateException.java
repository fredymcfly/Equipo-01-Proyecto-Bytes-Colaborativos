package com.fleetcontrol.msalerts.exception;

import com.fleetcontrol.msalerts.constants.ErrorConstants;
import java.io.Serial;
import org.springframework.http.HttpStatus;

/** Thrown when an alert is not in the state the operation needs. */
public class InvalidAlertStateException extends ApiException {

  @Serial private static final long serialVersionUID = 1L;

  /** Creates the exception with the standard code and message. */
  public InvalidAlertStateException() {
    super(
        HttpStatus.CONFLICT,
        ErrorConstants.Code.INVALID_ALERT_STATE,
        ErrorConstants.Message.INVALID_ALERT_STATE);
  }
}
