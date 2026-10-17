package com.fleetcontrol.msalerts.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Global configuration constants for the application. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ConfigConstants {

  public static final String BASE_PACKAGE = "com.fleetcontrol.msalerts";
  public static final String CLIENT_PACKAGE = BASE_PACKAGE + ".client";

  /** Prefixes of the configuration properties. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Properties {
    public static final String FLEET_JWT = "fleet.jwt";
    public static final String FLEET_INTERNAL = "fleet.internal";
    public static final String FLEET_SERVICES = "fleet.services";
    public static final String ALERTS_EVALUATION = "alerts.evaluation";
    public static final String ENABLED = "enabled";
  }

  /** Names of the services this one talks to. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Services {
    public static final String VEHICLES = "ms-vehicles";
    public static final String DRIVERS = "ms-drivers";
    public static final String MAINTENANCE = "ms-maintenance";
    public static final String FUEL = "ms-fuel";
  }

  /** Placeholders with the base URL of each source service. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Urls {
    public static final String VEHICLES = "${fleet.services.vehicles.url}";
    public static final String DRIVERS = "${fleet.services.drivers.url}";
    public static final String MAINTENANCE = "${fleet.services.maintenance.url}";
    public static final String FUEL = "${fleet.services.fuel.url}";
  }
}
