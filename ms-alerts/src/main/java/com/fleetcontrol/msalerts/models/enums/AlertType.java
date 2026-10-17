package com.fleetcontrol.msalerts.models.enums;

/** Situation an alert reports. The order of the constants is the order the rules are listed in. */
public enum AlertType {
  MAINTENANCE_DUE,
  MAINTENANCE_OVERDUE,
  LICENSE_EXPIRING,
  LICENSE_EXPIRED,
  HIGH_CONSUMPTION
}
