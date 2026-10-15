package com.fleetcontrol.msauth.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Constants related to JWT tokens and security. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SecurityConstants {

  /** Paths that can be called without authentication. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Path {
    public static final String[] PUBLIC = {
      ApiPaths.Auth.BASE + ApiPaths.Auth.REGISTER,
      ApiPaths.Auth.BASE + ApiPaths.Auth.LOGIN,
      ApiPaths.Auth.BASE + ApiPaths.Auth.VALIDATE,
      "/actuator/health",
      "/actuator/health/**",
      "/swagger-ui.html",
      "/swagger-ui/**",
      "/v3/api-docs/**"
    };
  }

  /** Names of the custom claims stored inside the JWT. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Claim {
    public static final String USERNAME = "username";
    public static final String ROLE = "role";
  }
}
