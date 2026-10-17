package com.fleetcontrol.msalerts.config.properties;

import com.fleetcontrol.msalerts.constants.ConfigConstants;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings of the scheduled evaluation.
 *
 * @param enabled whether the evaluator runs by itself; an ADMIN can still launch it by hand
 * @param intervalMs milliseconds between the end of one run and the start of the next
 * @param initialDelayMs milliseconds to wait before the first run, so the other services start
 */
@ConfigurationProperties(prefix = ConfigConstants.Properties.ALERTS_EVALUATION)
public record EvaluationProperties(boolean enabled, long intervalMs, long initialDelayMs) {}
