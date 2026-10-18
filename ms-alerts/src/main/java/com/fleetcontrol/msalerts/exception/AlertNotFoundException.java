package com.fleetcontrol.msalerts.exception;

import com.fleetcontrol.msalerts.constants.ErrorConstants;
import java.io.Serial;
import org.springframework.http.HttpStatus;

/** Thrown when an alert does not exist. */
public class AlertNotFoundException extends ApiException {

  @Serial private static final long serialVersionUID = 1L;

  /** Creates the exception with the standard code and message. */
  public AlertNotFoundException() {
    super(
        HttpStatus.NOT_FOUND,
        ErrorConstants.Code.ALERT_NOT_FOUND,
        ErrorConstants.Message.ALERT_NOT_FOUND);
  }
}
