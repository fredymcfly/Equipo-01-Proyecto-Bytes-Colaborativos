package com.fleetcontrol.msauth.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/** Base exception for errors that map directly to an HTTP status and an error code. */
@Getter
public class ApiException extends RuntimeException {

  private final HttpStatus status;
  private final String code;

  /**
   * Creates the exception.
   *
   * @param status HTTP status of the response
   * @param code machine-readable code returned in the {@code error} field
   * @param message human-readable message returned in the {@code message} field
   */
  public ApiException(HttpStatus status, String code, String message) {
    super(message);
    this.status = status;
    this.code = code;
  }
}
