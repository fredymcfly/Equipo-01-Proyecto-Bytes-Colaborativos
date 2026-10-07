package com.fleetcontrol.msvehicles.exception;

/** Base exception for domain errors that map to an error code of the shared contract. */
public class ApiException extends RuntimeException {

  private final ErrorCode code;

  /** Creates an exception with the default message of the given error code. */
  public ApiException(ErrorCode code) {
    super(code.defaultMessage());
    this.code = code;
  }

  /** Creates an exception with a specific message for the given error code. */
  public ApiException(ErrorCode code, String message) {
    super(message);
    this.code = code;
  }

  /** Error code that determines the HTTP status and the response body. */
  public ErrorCode code() {
    return code;
  }
}
