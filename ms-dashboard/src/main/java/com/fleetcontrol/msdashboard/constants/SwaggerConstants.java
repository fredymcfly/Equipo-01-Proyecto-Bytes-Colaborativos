package com.fleetcontrol.msdashboard.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Utility class holding the texts and examples of the OpenAPI documentation. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SwaggerConstants {

  /** General information of the API. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Api {
    public static final String TITLE = "FleetControl - ms-dashboard";
    public static final String DESCRIPTION =
        "Executive summary, time series and vehicle ranking of the fleet. It has no database: it "
            + "aggregates ms-vehicles, ms-routes, ms-fuel, ms-maintenance and ms-alerts, calling "
            + "them in parallel, and keeps each answer in a cache for 60 seconds.";
    public static final String VERSION = "1.0";
  }

  /** Names of the security schemes declared in the OpenAPI configuration. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Security {
    public static final String BEARER = "bearerAuth";
    public static final String BEARER_SCHEME = "bearer";
    public static final String BEARER_FORMAT = "JWT";
    public static final String BEARER_DESCRIPTION =
        "JWT signed with the shared secret. The role must be MANAGER or ADMIN. Internal calls "
            + "are not allowed.";
  }

  /** Tags that group the endpoints. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Tags {
    public static final String DASHBOARD = "Dashboard";
    public static final String DASHBOARD_DESCRIPTION =
        "Aggregated data of the fleet. If a source service does not answer, the endpoints return "
            + "what they have with partial set to true and the service listed in unavailable.";
  }

  /** Summaries and descriptions of the operations. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Operations {
    public static final String OVERVIEW = "Fleet summary";
    public static final String OVERVIEW_DESCRIPTION =
        "Main summary for the last days: status of the vehicles (ms-vehicles), key indicators "
            + "(ms-routes, ms-fuel and ms-maintenance), open alerts (ms-alerts) and the next 5 "
            + "pending maintenance orders (ms-maintenance). The source services are called in "
            + "parallel. If one does not answer, the response is still 200 with partial set to "
            + "true and the values that depend on it set to null.";
    public static final String TIMESERIES = "Time series of metrics";
    public static final String TIMESERIES_DESCRIPTION =
        "One series for each requested metric, all with the same granularity, ready for a line "
            + "or bar chart. The range can be at most 366 days. If a source service does not "
            + "answer, only its metrics are left out and it is listed in unavailable.";
    public static final String RANKING = "Vehicle ranking";
    public static final String RANKING_DESCRIPTION =
        "Vehicles sorted from the highest to the lowest fuel cost or consumption, built from "
            + "ms-fuel. The type limits the ranking to equivalent vehicles. A vehicle with no "
            + "distance has no consumption and is not ranked by CONSUMPTION. If ms-fuel does not "
            + "answer there is nothing to rank, so the response is 503.";
  }

  /** Descriptions of the request parameters. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Params {
    public static final String DAYS = "Days of history, from 1 to 365. The period ends today";
    public static final String METRICS =
        "Comma separated metrics: DISTANCE_KM, FUEL_LITERS, FUEL_COST, CONSUMPTION_L100KM, "
            + "MAINTENANCE_COST or ALERTS_CREATED";
    public static final String FROM = "First day of the range (YYYY-MM-DD)";
    public static final String TO = "Last day of the range (YYYY-MM-DD), at most 366 days later";
    public static final String GRANULARITY = "Size of each period: DAY, WEEK or MONTH";
    public static final String RANKING_METRIC = "Value used to sort: FUEL_COST or CONSUMPTION";
    public static final String LIMIT = "Number of vehicles to return, from 1 to 20";
    public static final String TYPE = "Vehicle type to limit the ranking to";
    public static final String EXAMPLE_METRICS = "DISTANCE_KM,FUEL_COST";
    public static final String EXAMPLE_FROM = "2026-09-07";
    public static final String EXAMPLE_TO = "2026-09-27";
  }

  /** HTTP status codes of the documented responses. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Status {
    public static final String OK = "200";
    public static final String BAD_REQUEST = "400";
    public static final String UNAUTHORIZED = "401";
    public static final String FORBIDDEN = "403";
    public static final String SERVICE_UNAVAILABLE = "503";
  }

  /** Descriptions of the documented responses. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Responses {
    public static final String OK = "Request completed. It can be partial, see partial";
    public static final String RANKING_OK = "Request completed";
    public static final String BAD_REQUEST = "VALIDATION_ERROR: a field or parameter is invalid";
    public static final String UNAUTHORIZED =
        "UNAUTHORIZED or TOKEN_EXPIRED: credentials are missing, invalid or expired";
    public static final String FORBIDDEN =
        "FORBIDDEN: the role cannot use this endpoint. Internal calls are not allowed";
    public static final String SERVICE_UNAVAILABLE =
        "SERVICE_UNAVAILABLE: ms-fuel does not answer, so nothing can be ranked";
  }

  /** Names of the examples that share a response. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class ExampleNames {
    public static final String COMPLETE = "Complete";
    public static final String PARTIAL = "ms-fuel unavailable";
  }

  /** JSON examples of responses. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Examples {
    public static final String OVERVIEW =
        """
        {
          "generatedAt": "2026-10-05T10:30:00Z",
          "period": { "from": "2026-09-05", "to": "2026-10-05" },
          "partial": false,
          "unavailable": [],
          "fleet": {
            "total": 25,
            "available": 14,
            "inUse": 8,
            "inMaintenance": 2,
            "outOfService": 1
          },
          "kpis": {
            "routesCompleted": 412,
            "distanceKm": 38420.5,
            "fuelLiters": 4226.3,
            "fuelCost": 6846.61,
            "avgConsumptionL100km": 11.0,
            "maintenanceCost": 2140.25
          },
          "alerts": { "open": 11, "critical": 2, "warning": 9, "info": 0 },
          "upcomingMaintenance": [
            {
              "orderId": "e5b8c3f7-91a2-4d60-b7c4-0a1f6d2e8b01",
              "vehicleId": "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a01",
              "plate": "4821-KDF",
              "type": "OIL_CHANGE",
              "scheduledFor": "2026-10-08"
            }
          ]
        }
        """;
    public static final String OVERVIEW_PARTIAL =
        """
        {
          "generatedAt": "2026-10-05T10:30:00Z",
          "period": { "from": "2026-09-05", "to": "2026-10-05" },
          "partial": true,
          "unavailable": ["ms-fuel"],
          "fleet": {
            "total": 25,
            "available": 14,
            "inUse": 8,
            "inMaintenance": 2,
            "outOfService": 1
          },
          "kpis": {
            "routesCompleted": 412,
            "distanceKm": 38420.5,
            "fuelLiters": null,
            "fuelCost": null,
            "avgConsumptionL100km": null,
            "maintenanceCost": 2140.25
          },
          "alerts": { "open": 11, "critical": 2, "warning": 9, "info": 0 },
          "upcomingMaintenance": []
        }
        """;
    public static final String TIMESERIES =
        """
        {
          "from": "2026-09-07",
          "to": "2026-09-27",
          "granularity": "WEEK",
          "partial": false,
          "unavailable": [],
          "series": [
            {
              "metric": "DISTANCE_KM",
              "unit": "km",
              "points": [
                { "period": "2026-09-07", "value": 9034.1 },
                { "period": "2026-09-14", "value": 9710.6 },
                { "period": "2026-09-21", "value": 9340.2 }
              ]
            },
            {
              "metric": "FUEL_COST",
              "unit": "EUR",
              "points": [
                { "period": "2026-09-07", "value": 1595.21 },
                { "period": "2026-09-14", "value": 1746.20 },
                { "period": "2026-09-21", "value": 1664.39 }
              ]
            }
          ]
        }
        """;
    public static final String TIMESERIES_PARTIAL =
        """
        {
          "from": "2026-09-07",
          "to": "2026-09-27",
          "granularity": "WEEK",
          "partial": true,
          "unavailable": ["ms-fuel"],
          "series": [
            {
              "metric": "DISTANCE_KM",
              "unit": "km",
              "points": [
                { "period": "2026-09-07", "value": 9034.1 },
                { "period": "2026-09-14", "value": 9710.6 },
                { "period": "2026-09-21", "value": 9340.2 }
              ]
            }
          ]
        }
        """;
    public static final String RANKING =
        """
        {
          "metric": "CONSUMPTION",
          "unit": "L/100 km",
          "period": { "from": "2026-09-05", "to": "2026-10-05" },
          "ranking": [
            {
              "rank": 1,
              "vehicleId": "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a03",
              "plate": "9012-PQR",
              "type": "TRUCK",
              "value": 27.5
            },
            {
              "rank": 2,
              "vehicleId": "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a01",
              "plate": "4821-KDF",
              "type": "VAN",
              "value": 10.9
            }
          ]
        }
        """;
    public static final String ERROR_VALIDATION =
        """
        {
          "error": "VALIDATION_ERROR",
          "message": "The request contains invalid fields",
          "details": [
            { "field": "to", "reason": "the range must not exceed 366 days" }
          ],
          "timestamp": "2026-10-05T10:30:00Z"
        }
        """;
    public static final String ERROR_UNAUTHORIZED =
        """
        {
          "error": "UNAUTHORIZED",
          "message": "Authentication is required",
          "timestamp": "2026-10-05T10:30:00Z"
        }
        """;
    public static final String ERROR_FORBIDDEN =
        """
        {
          "error": "FORBIDDEN",
          "message": "You do not have permission for this operation",
          "timestamp": "2026-10-05T10:30:00Z"
        }
        """;
    public static final String ERROR_SERVICE_UNAVAILABLE =
        """
        {
          "error": "SERVICE_UNAVAILABLE",
          "message": "A service needed to answer the request is not available",
          "service": "ms-fuel",
          "timestamp": "2026-10-05T10:30:00Z"
        }
        """;
  }
}
