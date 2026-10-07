package com.fleetcontrol.msroutes.exeption;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 *  Business or lookup error that maps to the common error contract.
 */
@Getter
public class ApiException extends RuntimeException{

  private final HttpStatus status;
  private final String code;

  public ApiException(HttpStatus status, String code, String message) {
    super(message);
    this.status = status;
    this.code = code;
  }
}
