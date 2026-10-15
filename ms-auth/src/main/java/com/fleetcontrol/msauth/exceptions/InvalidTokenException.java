package com.fleetcontrol.msauth.exceptions;

import com.fleetcontrol.msauth.constants.ErrorConstants;
import org.springframework.http.HttpStatus;

/** Thrown when the token is missing, malformed or has a wrong signature (401). */
public class InvalidTokenException extends ApiException {

  /** Creates the exception with the standard message. */
  public InvalidTokenException() {
    super(
        HttpStatus.UNAUTHORIZED,
        ErrorConstants.Code.UNAUTHORIZED,
        ErrorConstants.Message.MISSING_INVALID_TOKEN);
  }
}
