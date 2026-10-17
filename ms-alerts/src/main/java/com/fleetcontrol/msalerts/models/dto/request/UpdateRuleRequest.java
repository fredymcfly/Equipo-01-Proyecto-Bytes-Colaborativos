package com.fleetcontrol.msalerts.models.dto.request;

import com.fleetcontrol.msalerts.models.enums.AlertSeverity;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

/** New configuration of an alert rule. The parameters are checked against the type of the rule. */
public record UpdateRuleRequest(
    @NotNull Boolean enabled, @NotNull AlertSeverity severity, Map<String, Object> params) {}
