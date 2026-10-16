package com.fleetcontrol.msdrivers.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Utility class holding the API route paths of the service. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ApiPaths {

  /** Driver endpoints exposed by this service. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Drivers {
    public static final String BASE = "/api/drivers";
    public static final String ALL = BASE + "/**";
    public static final String DRIVER_ID = "/{driverId}";
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
