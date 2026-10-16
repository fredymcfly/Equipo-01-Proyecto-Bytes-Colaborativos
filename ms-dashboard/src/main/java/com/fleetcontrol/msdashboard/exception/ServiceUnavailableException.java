package com.fleetcontrol.msdashboard.exception;

import com.fleetcontrol.msdashboard.constants.ErrorConstants;
import java.io.Serial;
import org.springframework.http.HttpStatus;

/** Thrown when the only service that can answer a request does not respond. */
public class ServiceUnavailableException extends ApiException {

  @Serial private static final long serialVersionUID = 1L;

  private final String service;

  /** Creates the exception for the service that failed. */
  public ServiceUnavailableException(String service) {
    super(
        HttpStatus.SERVICE_UNAVAILABLE,
        ErrorConstants.Code.SERVICE_UNAVAILABLE,
        ErrorConstants.Message.SERVICE_UNAVAILABLE);
    this.service = service;
  }

  /** Name of the service that did not respond. */
  public String service() {
    return service;
  }
}
