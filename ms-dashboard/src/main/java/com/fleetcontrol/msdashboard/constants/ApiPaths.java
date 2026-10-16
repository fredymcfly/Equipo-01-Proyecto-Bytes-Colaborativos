package com.fleetcontrol.msdashboard.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Utility class holding the API route paths of the service and of the services it consumes. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ApiPaths {

  /** Dashboard endpoints exposed by this service. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Dashboard {
    public static final String BASE = "/api/dashboard";
    public static final String ALL = BASE + "/**";
    public static final String TIMESERIES = "/timeseries";
    public static final String VEHICLES_RANKING = "/vehicles/ranking";
  }

  /** Endpoints of the source services that feed the dashboard. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Sources {

    /** Endpoints of ms-vehicles. */
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class Vehicles {
      public static final String SUMMARY = "/api/vehicles/summary";
      public static final String VEHICLE_ID = "/api/vehicles/{vehicleId}";
    }

    /** Endpoints of ms-routes. */
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class Routes {
      public static final String STATS = "/api/routes/stats";
    }

    /** Endpoints of ms-fuel. */
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class Fuel {
      public static final String STATS = "/api/fuel/stats";
      public static final String CONSUMPTION = "/api/fuel/consumption";
    }

    /** Endpoints of ms-maintenance. */
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class Maintenance {
      public static final String STATS = "/api/maintenance/stats";
      public static final String ORDERS = "/api/maintenance/orders";
    }

    /** Endpoints of ms-alerts. */
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class Alerts {
      public static final String STATS = "/api/alerts/stats";
    }
  }

  /** Paths that need no authentication. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Public {
    public static final String ACTUATOR_HEALTH = "/actuator/health";
    public static final String ACTUATOR_HEALTH_ALL = "/actuator/health/**";
    public static final String SWAGGER_UI_HTML = "/swagger-ui.html";
    public static final String SWAGGER_UI_ALL = "/swagger-ui/**";
    public static final String API_DOCS = "/v3/api-docs";
    public static final String API_DOCS_ALL = "/v3/api-docs/**";
  }
}
