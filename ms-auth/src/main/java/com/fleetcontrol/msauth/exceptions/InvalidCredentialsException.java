package com.fleetcontrol.msauth.exceptions;

import com.fleetcontrol.msauth.constants.ErrorConstants;
import org.springframework.http.HttpStatus;

/** Thrown when the email or the password is wrong (401). It never tells which one. */
public class InvalidCredentialsException extends ApiException {

  /** Creates the exception with the standard message. */
  public InvalidCredentialsException() {
    super(
        HttpStatus.UNAUTHORIZED,
        ErrorConstants.Code.INVALID_CREDENTIALS,
        ErrorConstants.Message.INVALID_CREDENTIALS);
  }
}
