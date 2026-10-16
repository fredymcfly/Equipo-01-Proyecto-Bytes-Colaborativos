package com.fleetcontrol.msdashboard.exception;

import java.io.Serial;
import org.springframework.http.HttpStatus;

/** Base exception for domain errors that map to an error code of the shared contract. */
public class ApiException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  private final HttpStatus status;
  private final String code;

  /** Creates an exception with its HTTP status, the contract error code and its message. */
  public ApiException(HttpStatus status, String code, String message) {
    super(message);
    this.status = status;
    this.code = code;
  }

  /** HTTP status of the response. */
  public HttpStatus status() {
    return status;
  }

  /** Error code written in the {@code error} field of the response. */
  public String code() {
    return code;
  }
}
