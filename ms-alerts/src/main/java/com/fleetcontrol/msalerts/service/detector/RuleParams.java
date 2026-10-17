package com.fleetcontrol.msalerts.service.detector;

import com.fleetcontrol.msalerts.models.entities.AlertRule;

/** Reads the parameters of a rule. */
public final class RuleParams {

  private RuleParams() {}

  /** Integer value of a parameter, or the fallback if the rule has no number for it. */
  public static int intValue(AlertRule rule, String name, int fallback) {
    Object value = rule.getParams().get(name);
    return value instanceof Number number ? number.intValue() : fallback;
  }
}
