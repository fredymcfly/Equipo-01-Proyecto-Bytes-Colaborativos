package com.fleetcontrol.msroutes.exeption;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/** A dependency did not respond; the contract adds the failing service name. */
@Getter
public class ServiceUnavailableException extends ApiException {
  private final String service;

  /**
   * Creates an exception for an unavailable downstream service.
   *
   * @param service name of the unavailable service
   */
  public ServiceUnavailableException(String service) {
    super(
        HttpStatus.SERVICE_UNAVAILABLE,
        "SERVICE_UNAVAILABLE",
        "The service " + service + " is unavailable");
    this.service = service;
  }
}
