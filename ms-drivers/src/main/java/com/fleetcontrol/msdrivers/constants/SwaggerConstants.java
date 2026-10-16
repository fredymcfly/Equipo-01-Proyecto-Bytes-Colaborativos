package com.fleetcontrol.msdrivers.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Utility class holding the texts and examples of the OpenAPI documentation. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SwaggerConstants {

  /** General information of the API. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Api {
    public static final String TITLE = "FleetControl - ms-drivers";
    public static final String DESCRIPTION = "Fleet drivers with their license and their status.";
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
    public static final String DRIVERS = "Drivers";
    public static final String DRIVERS_DESCRIPTION =
        "Drivers of the fleet. ms-routes queries it to check that a driver can be assigned to "
            + "a route, and ms-alerts to detect licenses that are about to expire.";
  }

  /** Summaries and descriptions of the operations. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Operations {
    public static final String LIST = "List drivers";
    public static final String LIST_DESCRIPTION =
        "Paginated list of drivers. Every filter is optional. licenseExpiringInDays returns the "
            + "drivers whose license expires in that number of days or less, including the "
            + "licenses that are already expired.";
    public static final String GET = "Get a driver by id";
    public static final String GET_DESCRIPTION =
        "Detail of one driver, with the creation and update dates.";
    public static final String CREATE = "Create a driver";
    public static final String CREATE_DESCRIPTION =
        "Registers a driver. It is always created as ACTIVE, the license number must be unique "
            + "and the expiration date must be in the future.";
    public static final String UPDATE = "Update a driver";
    public static final String UPDATE_DESCRIPTION =
        "Updates every data of the driver, including the status (ACTIVE, ON_LEAVE or "
            + "SUSPENDED). It also renews the license with a new expiration date.";
  }

  /** Descriptions of the request parameters. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Params {
    public static final String STATUS = "Driver status to filter by";
    public static final String LICENSE_EXPIRING_IN_DAYS =
        "Drivers whose license expires in this number of days or less, already expired included";
    public static final String PAGE = "Page number, starting at 0";
    public static final String SIZE = "Page size, from 1 to 100";
    public static final String DRIVER_ID = "Id of the driver";
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
    public static final String CREATED = "Driver created";
    public static final String BAD_REQUEST = "VALIDATION_ERROR: a field or parameter is invalid";
    public static final String UNAUTHORIZED =
        "UNAUTHORIZED or TOKEN_EXPIRED: credentials are missing, invalid or expired";
    public static final String FORBIDDEN = "FORBIDDEN: the role cannot use this endpoint";
    public static final String NOT_FOUND = "DRIVER_NOT_FOUND: no driver exists with that id";
    public static final String LICENSE_CONFLICT =
        "DRIVER_ALREADY_EXISTS: a driver with that license number already exists";
  }

  /** JSON examples of requests and responses. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Examples {
    public static final String CREATE_REQUEST =
        """
        {
          "fullName": "Sara Ibáñez",
          "email": "sara.ibanez@fleetcontrol.com",
          "phone": "+34 622 456 789",
          "licenseNumber": "B-5120377",
          "licenseCategory": "B",
          "licenseExpiresAt": "2029-06-30"
        }
        """;
    public static final String UPDATE_REQUEST =
        """
        {
          "fullName": "Miguel Torres",
          "email": "miguel.torres@fleetcontrol.com",
          "phone": "+34 611 987 654",
          "licenseNumber": "C-2038841",
          "licenseCategory": "C",
          "licenseExpiresAt": "2031-10-20",
          "status": "ACTIVE"
        }
        """;
    public static final String DRIVER =
        """
        {
          "id": "3b2d8f10-7c4a-4e55-b1a0-5d9e2c7f1a01",
          "fullName": "Laura Fernández",
          "email": "laura.fernandez@fleetcontrol.com",
          "phone": "+34 600 123 456",
          "licenseNumber": "B-4471923",
          "licenseCategory": "B",
          "licenseExpiresAt": "2027-03-14",
          "status": "ACTIVE",
          "createdAt": "2026-07-01T08:00:00Z",
          "updatedAt": "2026-10-05T07:45:00Z"
        }
        """;
    public static final String DRIVER_PAGE =
        """
        {
          "content": [
            {
              "id": "3b2d8f10-7c4a-4e55-b1a0-5d9e2c7f1a01",
              "fullName": "Laura Fernández",
              "email": "laura.fernandez@fleetcontrol.com",
              "phone": "+34 600 123 456",
              "licenseNumber": "B-4471923",
              "licenseCategory": "B",
              "licenseExpiresAt": "2027-03-14",
              "status": "ACTIVE"
            },
            {
              "id": "3b2d8f10-7c4a-4e55-b1a0-5d9e2c7f1a02",
              "fullName": "Miguel Torres",
              "email": "miguel.torres@fleetcontrol.com",
              "phone": "+34 611 987 654",
              "licenseNumber": "C-2038841",
              "licenseCategory": "C",
              "licenseExpiresAt": "2026-10-20",
              "status": "ACTIVE"
            }
          ],
          "page": 0,
          "size": 20,
          "totalElements": 20,
          "totalPages": 1
        }
        """;
    public static final String ERROR_VALIDATION =
        """
        {
          "error": "VALIDATION_ERROR",
          "message": "The request contains invalid fields",
          "details": [
            { "field": "licenseExpiresAt", "reason": "must be a future date" }
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
          "error": "DRIVER_NOT_FOUND",
          "message": "No driver exists with the provided id",
          "timestamp": "2026-10-05T10:30:00Z"
        }
        """;
    public static final String ERROR_LICENSE_EXISTS =
        """
        {
          "error": "DRIVER_ALREADY_EXISTS",
          "message": "A driver with that license number already exists",
          "timestamp": "2026-10-05T10:30:00Z"
        }
        """;
  }
}
