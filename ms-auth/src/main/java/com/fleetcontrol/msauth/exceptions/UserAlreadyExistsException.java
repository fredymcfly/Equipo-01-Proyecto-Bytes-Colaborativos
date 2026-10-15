package com.fleetcontrol.msauth.exceptions;

import com.fleetcontrol.msauth.constants.ErrorConstants;
import org.springframework.http.HttpStatus;

/** Thrown when the email or the username is already registered (409). */
public class UserAlreadyExistsException extends ApiException {

  /** Creates the exception with the message that tells which field is duplicated. */
  public UserAlreadyExistsException(String message) {
    super(HttpStatus.CONFLICT, ErrorConstants.Code.USER_ALREADY_EXISTS, message);
  }
}
