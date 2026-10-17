package com.fleetcontrol.msalerts.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Utility class holding the texts and examples of the OpenAPI documentation. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SwaggerConstants {

  /** General information of the API. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Api {
    public static final String TITLE = "FleetControl - ms-alerts";
    public static final String DESCRIPTION =
        "Alerts about maintenance, driver licenses and fuel consumption. An evaluator checks "
            + "ms-maintenance, ms-drivers and ms-fuel every 15 minutes, creates the alerts and "
            + "resolves them when the condition disappears. The rules can be configured.";
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
        "JWT signed with the shared secret. The role must be MANAGER or ADMIN; some endpoints "
            + "need ADMIN.";
    public static final String INTERNAL_KEY_DESCRIPTION =
        "Key shared by the services. It only opens the list of alerts and the statistics.";
  }

  /** Tags that group the endpoints. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Tags {
    public static final String ALERTS = "Alerts";
    public static final String ALERTS_DESCRIPTION =
        "List, acknowledge and statistics of the alerts, and manual evaluation.";
    public static final String RULES = "Rules";
    public static final String RULES_DESCRIPTION =
        "Configuration of the rules the evaluator applies. A change is used from the next "
            + "evaluation.";
  }

  /** Summaries and descriptions of the operations. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Operations {
    public static final String LIST = "List alerts";
    public static final String LIST_DESCRIPTION =
        "Paginated list of alerts. Every filter is optional. They are sorted by severity, from "
            + "the most to the least severe, and then by creation date, from the newest. "
            + "Available to MANAGER, ADMIN and other services.";
    public static final String ACKNOWLEDGE = "Acknowledge an alert";
    public static final String ACKNOWLEDGE_DESCRIPTION =
        "Moves an open alert to ACKNOWLEDGED and stores who did it, when and the optional "
            + "comment. An alert in any other state cannot be acknowledged. Available to MANAGER "
            + "and ADMIN.";
    public static final String EVALUATE = "Evaluate now";
    public static final String EVALUATE_DESCRIPTION =
        "Runs the evaluation immediately instead of waiting for the next scheduled one. It "
            + "returns the alerts created and resolved and the types that were skipped because "
            + "a source service did not answer. Only ADMIN.";
    public static final String STATS = "Alert statistics";
    public static final String STATS_DESCRIPTION =
        "Alerts created over time with the breakdown by severity, and the alerts that are open "
            + "now. The periods are in UTC and the empty ones are included with zeros. The range "
            + "can be at most 366 days. Available to MANAGER, ADMIN and other services.";
    public static final String LIST_RULES = "List rules";
    public static final String LIST_RULES_DESCRIPTION =
        "Configuration of the five rules. Available to MANAGER and ADMIN.";
    public static final String UPDATE_RULE = "Update a rule";
    public static final String UPDATE_RULE_DESCRIPTION =
        "Enables or disables a rule, changes its severity and replaces its parameters. "
            + "MAINTENANCE_DUE and LICENSE_EXPIRING take daysBefore, HIGH_CONSUMPTION takes "
            + "thresholdPercent, and the other two take none. Only ADMIN.";
  }

  /** Descriptions of the request parameters. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Params {
    public static final String STATUS = "Only alerts with this status";
    public static final String SEVERITY = "Only alerts with this severity";
    public static final String TYPE = "Only alerts of this type";
    public static final String VEHICLE = "Only alerts about this vehicle";
    public static final String PAGE = "Page number, starting at 0";
    public static final String SIZE = "Page size, from 1 to 100";
    public static final String ALERT_ID = "Identifier of the alert";
    public static final String RULE_TYPE =
        "Type of the rule: MAINTENANCE_DUE, MAINTENANCE_OVERDUE, LICENSE_EXPIRING, "
            + "LICENSE_EXPIRED or HIGH_CONSUMPTION";
    public static final String FROM = "First day of the range (YYYY-MM-DD)";
    public static final String TO = "Last day of the range (YYYY-MM-DD), at most 366 days later";
    public static final String GRANULARITY = "Size of each period: DAY, WEEK or MONTH";
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
    public static final String NOT_FOUND = "404";
    public static final String CONFLICT = "409";
  }

  /** Descriptions of the documented responses. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Responses {
    public static final String OK = "Request completed";
    public static final String BAD_REQUEST = "VALIDATION_ERROR: a field or parameter is invalid";
    public static final String UNAUTHORIZED =
        "UNAUTHORIZED or TOKEN_EXPIRED: credentials are missing, invalid or expired";
    public static final String FORBIDDEN = "FORBIDDEN: the role cannot use this endpoint";
    public static final String ALERT_NOT_FOUND = "ALERT_NOT_FOUND: the alert does not exist";
    public static final String RULE_NOT_FOUND = "RULE_NOT_FOUND: the rule does not exist";
    public static final String INVALID_STATE =
        "INVALID_ALERT_STATE: the alert is not open, so it cannot be acknowledged";
  }

  /** JSON examples of requests and responses. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Examples {
    public static final String ALERT_PAGE =
        """
        {
          "content": [
            {
              "id": "d1a5c7e9-3f42-4b8a-a6d0-9e2b4c8f1a01",
              "type": "MAINTENANCE_OVERDUE",
              "severity": "CRITICAL",
              "status": "OPEN",
              "vehicleId": "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a05",
              "driverId": null,
              "message": "Mantenimiento OIL_CHANGE vencido desde el 2026-09-28 \
        en el vehículo 3342-HJK",
              "createdAt": "2026-10-05T06:15:00Z",
              "acknowledgedAt": null,
              "acknowledgedBy": null,
              "comment": null,
              "resolvedAt": null
            },
            {
              "id": "d1a5c7e9-3f42-4b8a-a6d0-9e2b4c8f1a02",
              "type": "LICENSE_EXPIRING",
              "severity": "WARNING",
              "status": "ACKNOWLEDGED",
              "vehicleId": null,
              "driverId": "3b2d8f10-7c4a-4e55-b1a0-5d9e2c7f1a02",
              "message": "La licencia de Miguel Torres caduca el 2026-10-20",
              "createdAt": "2026-10-04T06:15:00Z",
              "acknowledgedAt": "2026-10-04T09:12:00Z",
              "acknowledgedBy": "8a6e4c2d-1f3b-4a5c-9d7e-0b2c4e6f8a10",
              "comment": "Renovación en trámite",
              "resolvedAt": null
            }
          ],
          "page": 0,
          "size": 20,
          "totalElements": 2,
          "totalPages": 1
        }
        """;
    public static final String ACKNOWLEDGE_REQUEST =
        """
        {
          "comment": "Cita pedida con el taller para el jueves"
        }
        """;
    public static final String ALERT =
        """
        {
          "id": "d1a5c7e9-3f42-4b8a-a6d0-9e2b4c8f1a01",
          "type": "MAINTENANCE_OVERDUE",
          "severity": "CRITICAL",
          "status": "ACKNOWLEDGED",
          "vehicleId": "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a05",
          "driverId": null,
          "message": "Mantenimiento OIL_CHANGE vencido desde el 2026-09-28 en el vehículo 3342-HJK",
          "createdAt": "2026-10-05T06:15:00Z",
          "acknowledgedAt": "2026-10-05T08:40:00Z",
          "acknowledgedBy": "8a6e4c2d-1f3b-4a5c-9d7e-0b2c4e6f8a10",
          "comment": "Cita pedida con el taller para el jueves",
          "resolvedAt": null
        }
        """;
    public static final String RULES =
        """
        [
          { "type": "MAINTENANCE_DUE", "enabled": true, "severity": "WARNING",
            "params": { "daysBefore": 7 } },
          { "type": "MAINTENANCE_OVERDUE", "enabled": true, "severity": "CRITICAL",
            "params": {} },
          { "type": "LICENSE_EXPIRING", "enabled": true, "severity": "WARNING",
            "params": { "daysBefore": 30 } },
          { "type": "LICENSE_EXPIRED", "enabled": true, "severity": "CRITICAL",
            "params": {} },
          { "type": "HIGH_CONSUMPTION", "enabled": true, "severity": "WARNING",
            "params": { "thresholdPercent": 25 } }
        ]
        """;
    public static final String UPDATE_RULE_REQUEST =
        """
        {
          "enabled": true,
          "severity": "CRITICAL",
          "params": { "daysBefore": 14 }
        }
        """;
    public static final String RULE =
        """
        {
          "type": "MAINTENANCE_DUE",
          "enabled": true,
          "severity": "CRITICAL",
          "params": { "daysBefore": 14 }
        }
        """;
    public static final String EVALUATION =
        """
        {
          "created": 3,
          "resolved": 1,
          "skipped": ["HIGH_CONSUMPTION"],
          "durationMs": 842
        }
        """;
    public static final String STATS =
        """
        {
          "from": "2026-09-07",
          "to": "2026-09-27",
          "granularity": "WEEK",
          "totals": { "created": 11 },
          "series": [
            { "period": "2026-09-07", "created": 3,
              "bySeverity": { "INFO": 0, "WARNING": 2, "CRITICAL": 1 } },
            { "period": "2026-09-14", "created": 5,
              "bySeverity": { "INFO": 0, "WARNING": 3, "CRITICAL": 2 } },
            { "period": "2026-09-21", "created": 3,
              "bySeverity": { "INFO": 0, "WARNING": 2, "CRITICAL": 1 } }
          ],
          "open": { "INFO": 0, "WARNING": 9, "CRITICAL": 2 }
        }
        """;
    public static final String ERROR_VALIDATION =
        """
        {
          "error": "VALIDATION_ERROR",
          "message": "The request contains invalid fields",
          "details": [
            { "field": "params.daysBefore", "reason": "must be between 1 and 365" }
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
    public static final String ERROR_ALERT_NOT_FOUND =
        """
        {
          "error": "ALERT_NOT_FOUND",
          "message": "Alert not found",
          "timestamp": "2026-10-05T10:30:00Z"
        }
        """;
    public static final String ERROR_RULE_NOT_FOUND =
        """
        {
          "error": "RULE_NOT_FOUND",
          "message": "Alert rule not found",
          "timestamp": "2026-10-05T10:30:00Z"
        }
        """;
    public static final String ERROR_INVALID_STATE =
        """
        {
          "error": "INVALID_ALERT_STATE",
          "message": "Only an open alert can be acknowledged",
          "timestamp": "2026-10-05T10:30:00Z"
        }
        """;
  }
}
