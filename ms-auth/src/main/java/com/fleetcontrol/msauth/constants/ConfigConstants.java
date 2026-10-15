package com.fleetcontrol.msauth.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Global configuration constants for the application. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ConfigConstants {

  public static final String BASE_PACKAGE = "com.fleetcontrol.msauth";

  /** Prefixes of the configuration properties. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Properties {
    public static final String FLEET_JWT = "fleet.jwt";
    public static final String FLEET_DEMO = "fleet.demo";
    public static final String FLEET_INTERNAL = "fleet.internal";
  }

  /** Spring profiles. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Profiles {
    public static final String DEMO = "demo";
  }

  /** Values of the demo data seeder. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Demo {
    public static final String ADMIN_USERNAME = "admin";
    public static final String ADMIN_UUID_PREFIX = "fleetcontrol-ms-auth-admin-";
  }
}
