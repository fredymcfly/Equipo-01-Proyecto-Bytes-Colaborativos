package com.fleetcontrol.msalerts.models.dto.response;

import com.fleetcontrol.msalerts.models.entities.AlertRule;
import com.fleetcontrol.msalerts.models.enums.AlertSeverity;
import com.fleetcontrol.msalerts.models.enums.AlertType;
import java.util.Map;

/** Configuration of an alert rule. */
public record RuleResponse(
    AlertType type, boolean enabled, AlertSeverity severity, Map<String, Object> params) {

  /** Builds the representation of a rule. */
  public static RuleResponse from(AlertRule rule) {
    return new RuleResponse(rule.getType(), rule.isEnabled(), rule.getSeverity(), rule.getParams());
  }
}
