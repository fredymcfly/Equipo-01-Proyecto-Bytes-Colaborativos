package com.fleetcontrol.msdrivers.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Utility class holding security related constants. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SecurityConstants {

  /** Header that carries the key shared by the services. */
  public static final String INTERNAL_KEY_HEADER = "X-Internal-Key";

  public static final String DEFAULT_TOKEN_PREFIX = "Bearer";
  public static final String ROLE_AUTHORITY_PREFIX = "ROLE_";
  public static final String INTERNAL_PRINCIPAL = "internal-service";
  public static final String ROLE_MANAGER = "MANAGER";
  public static final String ROLE_ADMIN = "ADMIN";
  public static final String ROLE_CLAIM = "role";
  public static final String USERNAME_CLAIM = "username";
  public static final String EXPIRED_TOKEN_MARKER = "expired";
}
