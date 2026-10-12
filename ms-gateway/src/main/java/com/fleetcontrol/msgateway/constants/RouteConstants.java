package com.fleetcontrol.msgateway.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Centralized constants for API Gateway routing identifiers and resilience structures. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RouteConstants {

  /** Unique identifiers for each microservice route within the gateway. */
  public static final class Id {
    public static final String AUTH = "ms-auth-route";
    public static final String ROUTES = "ms-routes-route";
    public static final String VEHICLES = "ms-vehicles-route";
    public static final String FUEL = "ms-fuel-route";
    public static final String DRIVERS = "ms-drivers-route";
    public static final String MAINTENANCE = "ms-maintenance-route";
    public static final String ALERTS = "ms-alerts-route";
    public static final String DASHBOARD = "ms-dashboard-route";
  }

  /** Public path patterns the gateway exposes for each microservice. */
  public static final class GatewayRoute {
    public static final String AUTH = "/api/auth/**";
    public static final String ROUTES = "/api/routes/**";
    public static final String VEHICLES = "/api/vehicles/**";
    public static final String FUEL = "/api/fuel/**";
    public static final String DRIVERS = "/api/drivers/**";
    public static final String MAINTENANCE = "/api/maintenance/**";
    public static final String ALERTS = "/api/alerts/**";
    public static final String DASHBOARD = "/api/dashboard/**";
  }
}
