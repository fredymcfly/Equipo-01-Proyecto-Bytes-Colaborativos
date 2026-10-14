package com.fleetcontrol.msmaintenance.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Utility class holding the texts of the OpenAPI documentation. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SwaggerConstants {

  /** General information of the API. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Api {
    public static final String TITLE = "FleetControl - ms-maintenance";
    public static final String DESCRIPTION =
        "Maintenance plans, work orders and statistics of the fleet.";
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
    public static final String PLANS = "Maintenance plans";
    public static final String PLANS_DESCRIPTION =
        "Recurring maintenance plans of the vehicles, due by kilometers, by days or by both.";
    public static final String ORDERS = "Maintenance orders";
    public static final String ORDERS_DESCRIPTION =
        "Work orders generated from the plans: list them, start them and complete them.";
    public static final String STATS = "Maintenance statistics";
    public static final String STATS_DESCRIPTION =
        "Time series of completed maintenance consumed by ms-dashboard.";
  }

  /** Summaries and descriptions of the operations. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Operations {
    public static final String CREATE_PLAN = "Create a maintenance plan";
    public static final String CREATE_PLAN_DESCRIPTION =
        "Creates the plan of a vehicle. At least one interval (kilometers or days) is required, "
            + "and a vehicle can have only one active plan of each type. The next due values are "
            + "calculated from the last intervention.";
    public static final String LIST_PLANS = "List maintenance plans";
    public static final String LIST_PLANS_DESCRIPTION =
        "Paginated list ordered by next due date, with the plans that have no due date at the "
            + "end.";
    public static final String LIST_ORDERS = "List maintenance orders";
    public static final String LIST_ORDERS_DESCRIPTION =
        "Paginated list ordered by scheduled date. Every filter is optional.";
    public static final String START_ORDER = "Start a maintenance order";
    public static final String START_ORDER_DESCRIPTION =
        "Moves a pending order to in progress and puts its vehicle in maintenance.";
    public static final String COMPLETE_ORDER = "Complete a maintenance order";
    public static final String COMPLETE_ORDER_DESCRIPTION =
        "Completes an order in progress with its cost and odometer, releases the vehicle and "
            + "recalculates the next due values of the plan from the completion date.";
    public static final String STATS = "Maintenance statistics";
    public static final String STATS_DESCRIPTION =
        "Completed orders of a date range as a time series with totals, a breakdown by type and "
            + "the upcoming and overdue orders. Periods without orders are returned with zeros.";
  }

  /** Descriptions of the request parameters. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Params {
    public static final String VEHICLE = "Id of the vehicle to filter by";
    public static final String PLAN_TYPE = "Maintenance type to filter by";
    public static final String PLAN_ACTIVE = "Only active or only inactive plans";
    public static final String ORDER_STATUS = "Order status to filter by";
    public static final String ORDER_DUE_BEFORE =
        "Only orders scheduled on or before this date (yyyy-MM-dd)";
    public static final String ORDER_ID = "Id of the maintenance order";
    public static final String PAGE = "Page number, starting at 0";
    public static final String SIZE = "Page size, from 1 to 100";
    public static final String FROM = "First day of the range, included (yyyy-MM-dd, UTC)";
    public static final String TO = "Last day of the range, included (yyyy-MM-dd, UTC)";
    public static final String GRANULARITY = "Size of the periods of the series";
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
    public static final String SERVICE_UNAVAILABLE = "503";
  }

  /** Descriptions of the documented responses. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Responses {
    public static final String OK = "Request completed";
    public static final String CREATED = "Plan created";
    public static final String BAD_REQUEST = "VALIDATION_ERROR: a field or parameter is invalid";
    public static final String UNAUTHORIZED =
        "UNAUTHORIZED or TOKEN_EXPIRED: credentials are missing, invalid or expired";
    public static final String FORBIDDEN = "FORBIDDEN: the role cannot use this endpoint";
    public static final String PLAN_VEHICLE_NOT_FOUND =
        "VEHICLE_NOT_FOUND: ms-vehicles does not know the vehicle";
    public static final String ORDER_NOT_FOUND = "ORDER_NOT_FOUND: no order exists with that id";
    public static final String PLAN_CONFLICT =
        "PLAN_ALREADY_EXISTS: the vehicle already has an active plan of that type";
    public static final String START_CONFLICT =
        "INVALID_ORDER_STATE: the order is not pending, or VEHICLE_NOT_AVAILABLE: the vehicle "
            + "cannot go into maintenance";
    public static final String COMPLETE_CONFLICT =
        "INVALID_ORDER_STATE: the order is not in progress, or INVALID_ODOMETER: the odometer is "
            + "lower than the current one";
    public static final String SERVICE_UNAVAILABLE =
        "SERVICE_UNAVAILABLE: ms-vehicles does not respond";
  }
}
