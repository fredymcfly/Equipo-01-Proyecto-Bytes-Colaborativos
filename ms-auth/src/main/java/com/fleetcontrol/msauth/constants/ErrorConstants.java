package com.fleetcontrol.msauth.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Centralized error codes and messages, following the common error contract of the platform. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ErrorConstants {

  /** Machine-readable codes returned in the {@code error} field. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Code {
    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String UNAUTHORIZED = "UNAUTHORIZED";
    public static final String TOKEN_EXPIRED = "TOKEN_EXPIRED";
    public static final String INVALID_CREDENTIALS = "INVALID_CREDENTIALS";
    public static final String FORBIDDEN = "FORBIDDEN";
    public static final String USER_ALREADY_EXISTS = "USER_ALREADY_EXISTS";
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";
  }

  /** Human-readable messages returned in the {@code message} field. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Message {
    public static final String VALIDATION_ERROR = "The request contains invalid fields";
    public static final String INVALID_CREDENTIALS = "Invalid email or password";
    public static final String EMAIL_ALREADY_EXISTS = "A user with this email already exists";
    public static final String USERNAME_ALREADY_EXISTS = "A user with this username already exists";
    public static final String TOKEN_EXPIRED = "The token has expired";
    public static final String MISSING_INVALID_TOKEN = "Missing or invalid authorization token";
    public static final String ACCESS_DENIED = "Access denied, insufficient permissions";
    public static final String MALFORMED_BODY = "The request body is not valid";
    public static final String INTERNAL_ERROR = "Internal server error";
  }
}
