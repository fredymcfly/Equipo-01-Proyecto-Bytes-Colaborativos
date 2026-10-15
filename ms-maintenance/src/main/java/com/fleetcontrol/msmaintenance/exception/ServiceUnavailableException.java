package com.fleetcontrol.msmaintenance.exception;

import com.fleetcontrol.msmaintenance.constants.ErrorConstants;

/** Thrown when a service this one depends on does not respond. */
public class ServiceUnavailableException extends ApiException {

  private static final long serialVersionUID = 1L;

  private final String service;

  /** Creates the exception for the service that failed. */
  public ServiceUnavailableException(String service) {
    super(ErrorConstants.Code.SERVICE_UNAVAILABLE, ErrorConstants.Message.SERVICE_UNAVAILABLE);
    this.service = service;
  }

  /** Name of the service that did not respond. */
  public String service() {
    return service;
  }
}
