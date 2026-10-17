package com.fleetcontrol.msalerts.config;

import com.fleetcontrol.msalerts.config.properties.EvaluationProperties;
import com.fleetcontrol.msalerts.constants.ConfigConstants;
import com.fleetcontrol.msalerts.service.evaluation.AlertEvaluator;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.FixedDelayTask;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * Runs the evaluator on a fixed delay. The interval and the initial delay come from the
 * configuration, and the whole class is left out when {@code alerts.evaluation.enabled} is false.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(
    prefix = ConfigConstants.Properties.ALERTS_EVALUATION,
    name = ConfigConstants.Properties.ENABLED,
    havingValue = "true")
@RequiredArgsConstructor
public class EvaluationSchedulingConfig implements SchedulingConfigurer {

  private final AlertEvaluator evaluator;
  private final EvaluationProperties properties;

  @Override
  public void configureTasks(ScheduledTaskRegistrar registrar) {
    registrar.addFixedDelayTask(
        new FixedDelayTask(
            evaluator::evaluate,
            Duration.ofMillis(properties.intervalMs()),
            Duration.ofMillis(properties.initialDelayMs())));
  }
}
