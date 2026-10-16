package com.fleetcontrol.msdashboard.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Global configuration constants for the application. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ConfigConstants {

  public static final String BASE_PACKAGE = "com.fleetcontrol.msdashboard";
  public static final String CLIENT_PACKAGE = BASE_PACKAGE + ".client";

  /** Prefixes of the configuration properties. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Properties {
    public static final String FLEET_JWT = "fleet.jwt";
    public static final String FLEET_INTERNAL = "fleet.internal";
    public static final String FLEET_CACHE = "fleet.cache";
    public static final String FLEET_SERVICES = "fleet.services";
  }

  /** Names of the services this one talks to. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Services {
    public static final String VEHICLES = "ms-vehicles";
    public static final String ROUTES = "ms-routes";
    public static final String FUEL = "ms-fuel";
    public static final String MAINTENANCE = "ms-maintenance";
    public static final String ALERTS = "ms-alerts";
  }

  /** Placeholders with the base URL of each source service. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Urls {
    public static final String VEHICLES = "${fleet.services.vehicles.url}";
    public static final String ROUTES = "${fleet.services.routes.url}";
    public static final String FUEL = "${fleet.services.fuel.url}";
    public static final String MAINTENANCE = "${fleet.services.maintenance.url}";
    public static final String ALERTS = "${fleet.services.alerts.url}";
  }

  /** Thread pool that runs the calls to the source services in parallel. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Executor {
    public static final String BEAN_NAME = "sourceExecutor";
    public static final String THREAD_PREFIX = "source-";
    public static final int THREADS = 16;
    public static final int QUEUE_CAPACITY = 200;
  }
}
