package com.fleetcontrol.msroutes.exeption;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * A dependency did not respond; the contract adds the failing service name.
 */
@Getter
public class ServiceUnavailableException extends ApiException{
  private final String service;

  public ServiceUnavailableException(String service){
    super(
        HttpStatus.SERVICE_UNAVAILABLE,
        "SERVICE_UNAVAILABLE",
        "El servicio " + service + " no responde"
    );
    this.service = service;

  }
}
