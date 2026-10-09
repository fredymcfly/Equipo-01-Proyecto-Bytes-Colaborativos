package com.fleetcontrol.msroutes.exeption;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Represents an application error with the HTTP status and error code returned by the common API
 * error contract.
 */
@Getter
public class ApiException extends RuntimeException {

  private final HttpStatus status;
  private final String code;

  /**
   * Creates an API exception with the status, code, and message exposed to the client.
   *
   * @param status HTTP status associated with the error
   * @param code application-specific error code
   * @param message descriptive error message
   */
  public ApiException(HttpStatus status, String code, String message) {
    super(message);
    this.status = status;
    this.code = code;
  }
}
