package com.fleetcontrol.msvehicles.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Utility class holding the texts and examples of the OpenAPI documentation. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SwaggerConstants {

  /** General information of the API. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Api {
    public static final String TITLE = "FleetControl - ms-vehicles";
    public static final String DESCRIPTION =
        "Vehicle inventory of the fleet with its operational status and odometer.";
    public static final String VERSION = "1.0";
  }

  /** Names of the security schemes declared in the OpenAPI configuration. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Security {
    public static final String BEARER = "bearerAuth";
    public static final String INTERNAL_KEY = "internalKey";
    public static final String BEARER_SCHEME = "bearer";
    public static final String BEARER_FORMAT = "JWT";
    public static final String BEARER_DESCRIPTION =
        "JWT signed with the shared secret. The role must be MANAGER or ADMIN.";
    public static final String INTERNAL_KEY_DESCRIPTION =
        "Key shared by the services. It is treated as a MANAGER.";
  }

  /** Tags that group the endpoints. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Tags {
    public static final String VEHICLES = "Vehicles";
    public static final String VEHICLES_DESCRIPTION =
        "Inventory of the fleet vehicles. It is the source of truth that the other services "
            + "query, and they also send their status changes here.";
  }

  /** Summaries and descriptions of the operations. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Operations {
    public static final String LIST = "List vehicles";
    public static final String LIST_DESCRIPTION =
        "Paginated list of vehicles. Every filter is optional and the plate matches partially, "
            + "ignoring case.";
    public static final String SUMMARY = "Summarize the fleet";
    public static final String SUMMARY_DESCRIPTION =
        "Number of vehicles by status and by type. Statuses and types without vehicles are "
            + "returned with zero. It is consumed by ms-dashboard.";
    public static final String GET = "Get a vehicle by id";
    public static final String GET_DESCRIPTION = "Detail of one vehicle.";
    public static final String CREATE = "Create a vehicle";
    public static final String CREATE_DESCRIPTION =
        "Registers a vehicle. It is always created as AVAILABLE and its plate must be unique.";
    public static final String UPDATE = "Update a vehicle";
    public static final String UPDATE_DESCRIPTION =
        "Updates the descriptive data of a vehicle. The plate, the status and the odometer "
            + "are not modified.";
    public static final String CHANGE_STATUS = "Change the status of a vehicle";
    public static final String CHANGE_STATUS_DESCRIPTION =
        "Changes the status and, optionally, the odometer, which can only increase. Allowed "
            + "transitions: AVAILABLE <-> IN_USE, AVAILABLE <-> IN_MAINTENANCE, any status -> "
            + "OUT_OF_SERVICE, and OUT_OF_SERVICE -> AVAILABLE only for an ADMIN. ms-routes and "
            + "ms-maintenance call it with the internal key.";
  }

  /** Descriptions of the request parameters. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Params {
    public static final String STATUS = "Vehicle status to filter by";
    public static final String TYPE = "Vehicle type to filter by";
    public static final String PLATE = "Part of the plate to search for";
    public static final String PAGE = "Page number, starting at 0";
    public static final String SIZE = "Page size, from 1 to 100";
    public static final String VEHICLE_ID = "Id of the vehicle";
  }

  /** HTTP status codes of the documented responses. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Status {
    public static final String OK = "200";
    public static final String CREATED = "201";
    public static final String BAD_REQUEST = "400";
    public static final String UNAUTHORIZED = "401";
    public static final String FORBIDDEN = "403";
    public static final String NOT_FOUND = "404";
    public static final String CONFLICT = "409";
  }

  /** Descriptions of the documented responses. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Responses {
    public static final String OK = "Request completed";
    public static final String CREATED = "Vehicle created";
    public static final String BAD_REQUEST = "VALIDATION_ERROR: a field or parameter is invalid";
    public static final String UNAUTHORIZED =
        "UNAUTHORIZED or TOKEN_EXPIRED: credentials are missing, invalid or expired";
    public static final String FORBIDDEN =
        "FORBIDDEN: the role cannot use this endpoint, or only an ADMIN can reactivate a vehicle "
            + "that is out of service";
    public static final String NOT_FOUND = "VEHICLE_NOT_FOUND: no vehicle exists with that id";
    public static final String CREATE_CONFLICT =
        "VEHICLE_ALREADY_EXISTS: a vehicle with that plate already exists";
    public static final String STATUS_CONFLICT =
        "INVALID_STATUS_TRANSITION: the transition is not allowed, or INVALID_ODOMETER: the "
            + "odometer is lower than the current one";
  }

  /** Names of the examples shown in the documentation. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class ExampleNames {
    public static final String INVALID_TRANSITION = "Transition not allowed";
    public static final String INVALID_ODOMETER = "Odometer lower than the current one";
  }

  /** JSON examples of requests and responses. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Examples {
    public static final String CREATE_REQUEST =
        """
        {
          "plate": "9012-PQR",
          "make": "Mercedes-Benz",
          "model": "Actros",
          "year": 2023,
          "type": "TRUCK",
          "fuelType": "DIESEL",
          "tankCapacityL": 400,
          "odometerKm": 12000
        }
        """;
    public static final String UPDATE_REQUEST =
        """
        {
          "make": "Ford",
          "model": "Transit Custom",
          "year": 2023,
          "type": "VAN",
          "fuelType": "DIESEL",
          "tankCapacityL": 85
        }
        """;
    public static final String STATUS_REQUEST =
        """
        {
          "status": "AVAILABLE",
          "odometerKm": 45310
        }
        """;
    public static final String VEHICLE =
        """
        {
          "id": "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a01",
          "plate": "4821-KDF",
          "make": "Ford",
          "model": "Transit",
          "year": 2022,
          "type": "VAN",
          "fuelType": "DIESEL",
          "tankCapacityL": 80,
          "odometerKm": 45210,
          "status": "AVAILABLE",
          "createdAt": "2026-07-01T08:00:00Z",
          "updatedAt": "2026-10-05T07:45:00Z"
        }
        """;
    public static final String VEHICLE_PAGE =
        """
        {
          "content": [
            {
              "id": "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a01",
              "plate": "4821-KDF",
              "make": "Ford",
              "model": "Transit",
              "year": 2022,
              "type": "VAN",
              "fuelType": "DIESEL",
              "tankCapacityL": 80,
              "odometerKm": 45210,
              "status": "AVAILABLE"
            },
            {
              "id": "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a02",
              "plate": "7305-LMN",
              "make": "Renault",
              "model": "Clio",
              "year": 2021,
              "type": "CAR",
              "fuelType": "GASOLINE",
              "tankCapacityL": 45,
              "odometerKm": 61880,
              "status": "IN_USE"
            }
          ],
          "page": 0,
          "size": 20,
          "totalElements": 25,
          "totalPages": 2
        }
        """;
    public static final String FLEET_SUMMARY =
        """
        {
          "total": 25,
          "byStatus": {
            "AVAILABLE": 14,
            "IN_USE": 8,
            "IN_MAINTENANCE": 2,
            "OUT_OF_SERVICE": 1
          },
          "byType": {
            "CAR": 9,
            "VAN": 10,
            "TRUCK": 4,
            "MOTORCYCLE": 2
          }
        }
        """;
    public static final String ERROR_VALIDATION =
        """
        {
          "error": "VALIDATION_ERROR",
          "message": "The request contains invalid fields",
          "details": [
            { "field": "year", "reason": "must be between 1990 and next year" }
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
    public static final String ERROR_NOT_FOUND =
        """
        {
          "error": "VEHICLE_NOT_FOUND",
          "message": "No vehicle exists with the provided id",
          "timestamp": "2026-10-05T10:30:00Z"
        }
        """;
    public static final String ERROR_PLATE_EXISTS =
        """
        {
          "error": "VEHICLE_ALREADY_EXISTS",
          "message": "A vehicle with that plate already exists",
          "timestamp": "2026-10-05T10:30:00Z"
        }
        """;
    public static final String ERROR_INVALID_TRANSITION =
        """
        {
          "error": "INVALID_STATUS_TRANSITION",
          "message": "The status transition is not allowed",
          "timestamp": "2026-10-05T10:30:00Z"
        }
        """;
    public static final String ERROR_INVALID_ODOMETER =
        """
        {
          "error": "INVALID_ODOMETER",
          "message": "The odometer cannot be lower than the current one",
          "timestamp": "2026-10-05T10:30:00Z"
        }
        """;
  }
}
