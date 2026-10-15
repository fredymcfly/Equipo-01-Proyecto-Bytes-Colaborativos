package com.fleetcontrol.msauth.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Names and values used in the OpenAPI (Swagger) documentation. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class OpenApiConstants {

  public static final String BEARER_AUTH = "bearerAuth";

  /** HTTP response codes documented in the endpoints. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Code {
    public static final String SUCCESS = "200";
    public static final String CREATED = "201";
    public static final String BAD_REQUEST = "400";
    public static final String UNAUTHORIZED = "401";
    public static final String CONFLICT = "409";
  }
}
