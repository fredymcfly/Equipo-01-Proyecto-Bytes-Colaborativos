package com.fleetcontrol.msalerts.support;

import com.fleetcontrol.msalerts.models.entities.AlertRule;
import com.fleetcontrol.msalerts.models.enums.AlertSeverity;
import com.fleetcontrol.msalerts.models.enums.AlertType;
import java.util.HashMap;
import java.util.Map;

/** Builds rules for the unit tests. */
public final class Rules {

  private Rules() {}

  /** Enabled rule of the given type with its default severity and no parameters. */
  public static AlertRule rule(AlertType type) {
    return rule(type, defaultSeverity(type), Map.of());
  }

  /** Enabled rule of the given type with one parameter. */
  public static AlertRule rule(AlertType type, String param, int value) {
    return rule(type, defaultSeverity(type), Map.of(param, value));
  }

  /** Enabled rule with the given severity and parameters. */
  public static AlertRule rule(AlertType type, AlertSeverity severity, Map<String, Object> params) {
    AlertRule rule = new AlertRule();
    rule.setType(type);
    rule.setEnabled(true);
    rule.setSeverity(severity);
    rule.setParams(new HashMap<>(params));
    return rule;
  }

  private static AlertSeverity defaultSeverity(AlertType type) {
    return type == AlertType.MAINTENANCE_OVERDUE || type == AlertType.LICENSE_EXPIRED
        ? AlertSeverity.CRITICAL
        : AlertSeverity.WARNING;
  }
}
