package com.fleetcontrol.msvehicles.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Utility class holding the API route paths of the service. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ApiPaths {

  /** Vehicle endpoints exposed by this service. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Vehicles {
    public static final String BASE = "/api/vehicles";
    public static final String ALL = BASE + "/**";
    public static final String SUMMARY = "/summary";
    public static final String VEHICLE_ID = "/{vehicleId}";
    public static final String VEHICLE_ID_STATUS = "/{vehicleId}/status";
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
