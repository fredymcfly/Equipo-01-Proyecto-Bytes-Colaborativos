package com.fleetcontrol.msalerts.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Utility class holding the API route paths of the service and of the services it consumes. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ApiPaths {

  /** Alert endpoints exposed by this service. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Alerts {
    public static final String BASE = "/api/alerts";
    public static final String ALL = BASE + "/**";
    public static final String ALERT_ID = "/{alertId}";
    public static final String ACKNOWLEDGE = ALERT_ID + "/acknowledge";
    public static final String RULES = "/rules";
    public static final String RULE_TYPE = RULES + "/{type}";
    public static final String EVALUATE = "/evaluate";
    public static final String STATS = "/stats";

    /** Full paths used by the security rules. */
    public static final String FULL_STATS = BASE + STATS;

    public static final String FULL_RULES = BASE + RULES;
    public static final String FULL_RULES_ALL = FULL_RULES + "/**";
    public static final String FULL_EVALUATE = BASE + EVALUATE;
  }

  /** Endpoints of the source services that feed the evaluator. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Sources {

    /** Endpoints of ms-vehicles. */
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class Vehicles {
      public static final String VEHICLE_ID = "/api/vehicles/{vehicleId}";
    }

    /** Endpoints of ms-drivers. */
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class Drivers {
      public static final String LIST = "/api/drivers";
    }

    /** Endpoints of ms-maintenance. */
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class Maintenance {
      public static final String ORDERS = "/api/maintenance/orders";
    }

    /** Endpoints of ms-fuel. */
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class Fuel {
      public static final String CONSUMPTION = "/api/fuel/consumption";
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
