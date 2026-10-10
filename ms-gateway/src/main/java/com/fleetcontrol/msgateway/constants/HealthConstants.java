package com.fleetcontrol.msgateway.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Status values, paths and service names used by the gateway health endpoint. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class HealthConstants {
  public static final String UP = "UP";
  public static final String DOWN = "DOWN";
  public static final String ACTUATOR_HEALTH_PATH = "/actuator/health";

  /** Names under which each downstream service is reported. */
  public static final class ServiceName {
    public static final String AUTH = "ms-auth";
    public static final String VEHICLES = "ms-vehicles";
    public static final String DRIVERS = "ms-drivers";
    public static final String ROUTES = "ms-routes";
    public static final String MAINTENANCE = "ms-maintenance";
    public static final String FUEL = "ms-fuel";
    public static final String ALERTS = "ms-alerts";
    public static final String DASHBOARD = "ms-dashboard";
  }

  /** JSON examples shown in Swagger for the health endpoint. */
  public static final class Examples {
    public static final String HEALTH =
        """
        {
          "gateway": "UP",
          "timestamp": "2026-10-05T10:30:00Z",
          "services": {
            "ms-auth": "UP",
            "ms-vehicles": "UP",
            "ms-drivers": "UP",
            "ms-routes": "UP",
            "ms-maintenance": "UP",
            "ms-fuel": "UP",
            "ms-alerts": "UP",
            "ms-dashboard": "DOWN"
          }
        }
        """;

    public static final String RATE_LIMIT_EXCEEDED =
        """
        {
          "error": "RATE_LIMIT_EXCEEDED",
          "message": "Too many requests. Limit: 60 req/min",
          "retryAfter": 30,
          "timestamp": "2026-10-05T10:30:00Z"
        }
        """;
  }
}
