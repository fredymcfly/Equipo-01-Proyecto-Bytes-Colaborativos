package com.fleetcontrol.msmaintenance.exception;

import java.io.Serial;

/** Base exception for domain errors that map to an error code of the shared contract. */
public class ApiException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  private final String code;

  /** Creates an exception with the contract error code and its message. */
  public ApiException(String code, String message) {
    super(message);
    this.code = code;
  }

  /** Error code written in the {@code error} field of the response. */
  public String code() {
    return code;
  }
}
