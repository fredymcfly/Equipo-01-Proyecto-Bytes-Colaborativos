package com.fleetcontrol.msvehicles.exception;

import com.fleetcontrol.msvehicles.constants.ErrorConstants;
import java.io.Serial;
import org.springframework.http.HttpStatus;

/** Thrown when the current user is not allowed to perform the operation. */
public class ForbiddenException extends ApiException {

  @Serial private static final long serialVersionUID = 1L;

  /** Creates the exception with the default message. */
  public ForbiddenException() {
    super(HttpStatus.FORBIDDEN, ErrorConstants.Code.FORBIDDEN, ErrorConstants.Message.FORBIDDEN);
  }
}
