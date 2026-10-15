package com.fleetcontrol.msauth.exceptions;

import com.fleetcontrol.msauth.constants.ErrorConstants;
import org.springframework.http.HttpStatus;

/** Thrown when the token is correctly signed but has expired (401). */
public class TokenExpiredException extends ApiException {

  /** Creates the exception with the standard message. */
  public TokenExpiredException() {
    super(
        HttpStatus.UNAUTHORIZED,
        ErrorConstants.Code.TOKEN_EXPIRED,
        ErrorConstants.Message.TOKEN_EXPIRED);
  }
}
