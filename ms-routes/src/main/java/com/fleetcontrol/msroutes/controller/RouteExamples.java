package com.fleetcontrol.msroutes.controller;

/** JSON examples shown in Swagger UI for the route endpoints. */
public class RouteExamples {
  private RouteExamples() {}

  // ---------- Successful responses ----------

  static final String ROUTE_PLANNED =
      """
    {
      "id": "c8e1b7a4-52d9-4f06-8e3b-1a7d90c4f201",
      "vehicleId": "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a01",
      "driverId": "3b2d8f10-7c4a-4e55-b1a0-5d9e2c7f1a01",
      "origin": "Madrid - Centro Logístico",
      "destination": "Valencia - Puerto",
      "plannedStart": "2026-10-06T07:00:00Z",
      "estimatedDurationMin": 240,
      "plannedDistanceKm": 355.0,
      "status": "PLANNED"
    }
      """;

  static final String ROUTE_COMPLETED =
      """
    {
      "id": "c8e1b7a4-52d9-4f06-8e3b-1a7d90c4f201",
      "vehicleId": "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a01",
      "driverId": "3b2d8f10-7c4a-4e55-b1a0-5d9e2c7f1a01",
      "origin": "Madrid - Centro Logístico",
      "destination": "Valencia - Puerto",
      "plannedStart": "2026-10-06T07:00:00Z",
      "estimatedDurationMin": 240,
      "plannedDistanceKm": 355.0,
      "status": "COMPLETED",
      "startedAt": "2026-10-06T07:04:12Z",
      "endedAt": "2026-10-06T11:20:45Z",
      "startOdometerKm": 45210,
      "endOdometerKm": 45572,
      "actualDistanceKm": 362.4,
      "notes": "Sin incidencias"
    }
      """;

  static final String ROUTE_IN_PROGRESS =
      """
    {
      "id": "c8e1b7a4-52d9-4f06-8e3b-1a7d90c4f201",
      "vehicleId": "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a01",
      "driverId": "3b2d8f10-7c4a-4e55-b1a0-5d9e2c7f1a01",
      "origin": "Madrid - Centro Logístico",
      "destination": "Valencia - Puerto",
      "plannedStart": "2026-10-06T07:00:00Z",
      "estimatedDurationMin": 240,
      "plannedDistanceKm": 355.0,
      "status": "IN_PROGRESS",
      "startedAt": "2026-10-06T07:04:12Z",
      "startOdometerKm": 45210
    }
      """;

  static final String COMPLETE_REQUEST =
      """
    {
      "actualDistanceKm": 362.4,
      "notes": "Sin incidencias"
    }
      """;

  static final String ROUTE_STATS =
      """
    {
      "from": "2026-09-07",
      "to": "2026-09-27",
      "granularity": "WEEK",
      "totals": { "routes": 302, "distanceKm": 28084.9 },
      "series": [
        { "period": "2026-09-07", "routes": 97, "distanceKm": 9034.1, "avgDurationMin": 128 },
        { "period": "2026-09-14", "routes": 104, "distanceKm": 9710.6, "avgDurationMin": 131 },
        { "period": "2026-09-21", "routes": 101, "distanceKm": 9340.2, "avgDurationMin": 126 }
      ]
    }
      """;

  static final String ROUTE_PAGE =
      """
    {
      "content": [
        {
          "id": "d2f4a6c8-1b3d-4f5a-8c7e-9a0b1c2d3e4f",
          "vehicleId": "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a01",
          "driverId": "3b2d8f10-7c4a-4e55-b1a0-5d9e2c7f1a01",
          "origin": "Madrid - Centro Logístico",
          "destination": "Zaragoza - Plataforma",
          "plannedStart": "2026-10-08T06:30:00Z",
          "estimatedDurationMin": 200,
          "plannedDistanceKm": 320.5,
          "status": "PLANNED"
        },
        {
          "id": "c8e1b7a4-52d9-4f06-8e3b-1a7d90c4f201",
          "vehicleId": "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a01",
          "driverId": "3b2d8f10-7c4a-4e55-b1a0-5d9e2c7f1a01",
          "origin": "Madrid - Centro Logístico",
          "destination": "Valencia - Puerto",
          "plannedStart": "2026-10-06T07:00:00Z",
          "estimatedDurationMin": 240,
          "plannedDistanceKm": 355.0,
          "status": "COMPLETED",
          "startedAt": "2026-10-06T07:04:12Z",
          "endedAt": "2026-10-06T11:20:45Z",
          "startOdometerKm": 45210,
          "endOdometerKm": 45572,
          "actualDistanceKm": 362.4,
          "notes": "Sin incidencias"
        }
      ],
      "page": 0,
      "size": 20,
      "totalElements": 2,
      "totalPages": 1
    }
      """;

  // ---------- 400 / 401 / 403 ----------

  static final String ERR_VALIDATION =
      """
    {
      "error": "VALIDATION_ERROR",
      "message": "The request contains invalid values",
      "details": [
        { "field": "estimatedDurationMin", "reason": "must be greater than 0" },
        { "field": "origin", "reason": "must not be blank" }
      ],
      "timestamp": "2026-10-05T10:30:00Z"
    }
      """;

  static final String ERR_VALIDATION_PARAMS =
      """
    {
      "error": "VALIDATION_ERROR",
      "message": "The request contains invalid values",
      "details": [
        { "field": "size", "reason": "must be less than or equal to 100" }
      ],
      "timestamp": "2026-10-05T10:30:00Z"
    }
      """;

  static final String ERR_UNAUTHORIZED =
      """
    {
      "error": "UNAUTHORIZED",
      "message": "A valid authentication token is required",
      "timestamp": "2026-10-05T10:30:00Z"
    }
      """;

  static final String ERR_TOKEN_EXPIRED =
      """
    {
      "error": "TOKEN_EXPIRED",
      "message": "The token has expired",
      "timestamp": "2026-10-05T10:30:00Z"
    }
      """;

  static final String ERR_FORBIDDEN =
      """
    {
      "error": "FORBIDDEN",
      "message": "The user's role does not have permission for this operation",
      "timestamp": "2026-10-05T10:30:00Z"
    }
      """;

  // ---------- 404 ----------

  static final String ERR_VEHICLE_NOT_FOUND =
      """
    {
      "error": "VEHICLE_NOT_FOUND",
      "message": "No vehicle was found with the provided id.",
      "timestamp": "2026-10-05T10:30:00Z"
    }
      """;

  static final String ERR_DRIVER_NOT_FOUND =
      """
    {
      "error": "DRIVER_NOT_FOUND",
      "message": "No driver was found with the provided id.",
      "timestamp": "2026-10-05T10:30:00Z"
    }
      """;

  static final String ERR_ROUTE_NOT_FOUND =
      """
    {
      "error": "ROUTE_NOT_FOUND",
      "message": "No route was found with the provided id.",
      "timestamp": "2026-10-05T10:30:00Z"
    }
      """;

  // ---------- 409 ----------

  static final String ERR_VEHICLE_NOT_AVAILABLE =
      """
    {
      "error": "VEHICLE_NOT_AVAILABLE",
      "message": "The selected vehicle is out of service",
      "timestamp": "2026-10-05T10:30:00Z"
    }
      """;

  static final String ERR_DRIVER_INACTIVE =
      """
    {
      "error": "DRIVER_NOT_ELIGIBLE",
      "message": "Driver is not active",
      "timestamp": "2026-10-05T10:30:00Z"
    }
      """;

  static final String ERR_LICENSE_EXPIRED =
      """
    {
      "error": "DRIVER_NOT_ELIGIBLE",
      "message": "The driver's license is not valid on the start date",
      "timestamp": "2026-10-05T10:30:00Z"
    }
      """;

  static final String ERR_LICENSE_CATEGORY =
      """
    {
      "error": "DRIVER_NOT_ELIGIBLE",
      "message": "The license category is invalid for the selected vehicle type",
      "timestamp": "2026-10-05T10:30:00Z"
    }
      """;

  static final String ERR_VEHICLE_OVERLAP =
      """
    {
      "error": "ROUTE_OVERLAP",
      "message": "Vehicle unavailable: an existing route overlaps the specified time interval",
      "timestamp": "2026-10-05T10:30:00Z"
    }
      """;

  static final String ERR_DRIVER_OVERLAP =
      """
    {
      "error": "ROUTE_OVERLAP",
      "message": "Driver unavailable: an existing route overlaps the specified time interval",
      "timestamp": "2026-10-05T10:30:00Z"
    }
      """;

  // ---------- 503 ----------

  static final String ERR_SERVICE_UNAVAILABLE =
      """
    {
      "error": "SERVICE_UNAVAILABLE",
      "message": "The service ms-vehicles is unavailable",
      "service": "ms-vehicles",
      "timestamp": "2026-10-05T10:30:00Z"
    }
      """;

  static final String ERR_INVALID_ROUTE_STATE =
      """
    {
      "error": "INVALID_ROUTE_STATE",
      "message": "Only a planned route can be started",
      "timestamp": "2026-10-05T10:30:00Z"
    }
      """;
}
