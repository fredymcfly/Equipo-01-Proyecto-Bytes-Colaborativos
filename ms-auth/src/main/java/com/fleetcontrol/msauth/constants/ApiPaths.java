package com.fleetcontrol.msauth.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Paths of the REST API. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ApiPaths {

  /** Paths of the authentication endpoints. The endpoint paths are relative to {@code BASE}. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Auth {
    public static final String BASE = "/api/auth";
    public static final String REGISTER = "/register";
    public static final String LOGIN = "/login";
    public static final String VALIDATE = "/validate";
  }
}
