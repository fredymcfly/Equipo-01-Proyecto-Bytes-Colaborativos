package com.fleetcontrol.msmaintenance.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Global configuration constants for the application. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ConfigConstants {

  public static final String BASE_PACKAGE = "com.fleetcontrol.msmaintenance";

  /** Prefixes of the configuration properties. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Properties {
    public static final String FLEET_JWT = "fleet.jwt";
    public static final String FLEET_DEMO = "fleet.demo";
    public static final String FLEET_INTERNAL = "fleet.internal";
  }

  /** Names of the services this one talks to. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Services {
    public static final String VEHICLES = "ms-vehicles";
  }
}
