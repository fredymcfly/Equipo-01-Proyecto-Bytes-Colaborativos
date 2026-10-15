package com.fleetcontrol.msmaintenance.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Utility class holding API route paths and endpoints. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ApiPaths {

  /** Maintenance endpoints exposed by this service. */
  public static final class Maintenance {
    public static final String ALL = "/api/maintenance/**";
    public static final String PLANS = "/api/maintenance/plans";
    public static final String ORDERS = "/api/maintenance/orders";
    public static final String ORDER_START = "/api/maintenance/orders/{orderId}/start";
    public static final String ORDER_COMPLETE = "/api/maintenance/orders/{orderId}/complete";
    public static final String STATS = "/api/maintenance/stats";
  }

  /** Vehicle endpoints of ms-vehicles. */
  public static final class Vehicles {
    public static final String VEHICLE_ID = "/api/vehicles/{vehicleId}";
    public static final String VEHICLE_ID_STATUS = "/api/vehicles/{vehicleId}/status";
  }

  /** Paths that need no authentication. */
  public static final class Public {
    public static final String ACTUATOR_HEALTH = "/actuator/health";
    public static final String ACTUATOR_HEALTH_ALL = "/actuator/health/**";
    public static final String SWAGGER_UI_HTML = "/swagger-ui.html";
    public static final String SWAGGER_UI_ALL = "/swagger-ui/**";
    public static final String API_DOCS = "/v3/api-docs";
    public static final String API_DOCS_ALL = "/v3/api-docs/**";
  }
}
