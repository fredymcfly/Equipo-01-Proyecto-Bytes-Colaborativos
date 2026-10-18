package com.fleetcontrol.msalerts.service.evaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fleetcontrol.msalerts.models.dto.internal.Detection;
import com.fleetcontrol.msalerts.models.dto.response.EvaluationResponse;
import com.fleetcontrol.msalerts.models.entities.AlertRule;
import com.fleetcontrol.msalerts.models.enums.AlertSeverity;
import com.fleetcontrol.msalerts.models.enums.AlertType;
import com.fleetcontrol.msalerts.repository.AlertRuleRepository;
import com.fleetcontrol.msalerts.service.detector.AlertDetector;
import com.fleetcontrol.msalerts.service.detector.PlateResolver;
import com.fleetcontrol.msalerts.support.FeignFailures;
import com.fleetcontrol.msalerts.support.Rules;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests of the orchestration of an evaluation. */
@ExtendWith(MockitoExtension.class)
class AlertEvaluatorTest {

  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-10-05T10:30:00Z"), ZoneOffset.UTC);
  private static final LocalDate TODAY = LocalDate.of(2026, 10, 5);
  private static final Function<UUID, String> LOOKUP = vehicleId -> "3342-HJK";

  @Mock private AlertRuleRepository ruleRepository;
  @Mock private AlertDetector maintenance;
  @Mock private AlertDetector consumption;
  @Mock private PlateResolver plateResolver;
  @Mock private AlertReconciler reconciler;
  @Captor private ArgumentCaptor<Set<AlertType>> evaluated;
  @Captor private ArgumentCaptor<List<Detection>> detections;
  @Captor private ArgumentCaptor<Map<AlertType, AlertRule>> rulesGiven;

  private AlertEvaluator evaluator;

  @BeforeEach
  void setUp() {
    evaluator =
        new AlertEvaluator(
            ruleRepository, List.of(maintenance, consumption), plateResolver, reconciler, CLOCK);
    when(maintenance.types())
        .thenReturn(EnumSet.of(AlertType.MAINTENANCE_DUE, AlertType.MAINTENANCE_OVERDUE));
    when(consumption.types()).thenReturn(EnumSet.of(AlertType.HIGH_CONSUMPTION));
    when(plateResolver.newLookup()).thenReturn(LOOKUP);
    when(ruleRepository.findAll()).thenReturn(rules());
  }

  @Test
  void evaluate_givesEachDetectorItsEnabledRulesAndReconcilesTheResult() {
    Detection overdue = detection(AlertType.MAINTENANCE_OVERDUE, "MAINTENANCE_OVERDUE:1");
    Detection high = detection(AlertType.HIGH_CONSUMPTION, "HIGH_CONSUMPTION:2");
    when(maintenance.detect(any(), any(), any())).thenReturn(List.of(overdue));
    when(consumption.detect(any(), any(), any())).thenReturn(List.of(high));
    when(reconciler.reconcile(anySet(), anyList()))
        .thenReturn(new AlertReconciler.Reconciliation(2, 1));

    EvaluationResponse response = evaluator.evaluate();

    assertThat(response.created()).isEqualTo(2);
    assertThat(response.resolved()).isEqualTo(1);
    assertThat(response.skipped()).isEmpty();
    assertThat(response.durationMs()).isNotNegative();
    verify(maintenance).detect(rulesGiven.capture(), eq(TODAY), eq(LOOKUP));
    assertThat(rulesGiven.getValue().keySet())
        .containsExactlyInAnyOrder(AlertType.MAINTENANCE_DUE, AlertType.MAINTENANCE_OVERDUE);
    verify(reconciler).reconcile(evaluated.capture(), detections.capture());
    assertThat(evaluated.getValue()).containsExactlyInAnyOrder(allTypes());
    assertThat(detections.getValue()).containsExactlyInAnyOrder(overdue, high);
  }

  @Test
  void evaluate_withEveryRuleOfDetectorDisabled_doesNotCallItButStillEvaluatesItsTypes() {
    when(ruleRepository.findAll())
        .thenReturn(
            List.of(
                Rules.rule(AlertType.MAINTENANCE_DUE, "daysBefore", 7),
                Rules.rule(AlertType.MAINTENANCE_OVERDUE),
                disabled(AlertType.HIGH_CONSUMPTION)));
    when(maintenance.detect(any(), any(), any())).thenReturn(List.of());
    when(reconciler.reconcile(anySet(), anyList()))
        .thenReturn(new AlertReconciler.Reconciliation(0, 3));

    EvaluationResponse response = evaluator.evaluate();

    assertThat(response.resolved()).isEqualTo(3);
    verify(consumption, never()).detect(any(), any(), any());
    verify(reconciler).reconcile(evaluated.capture(), detections.capture());
    assertThat(evaluated.getValue()).contains(AlertType.HIGH_CONSUMPTION);
    assertThat(detections.getValue()).isEmpty();
  }

  @Test
  void evaluate_withMissingRule_treatsItAsDisabled() {
    when(ruleRepository.findAll())
        .thenReturn(
            List.of(
                Rules.rule(AlertType.MAINTENANCE_DUE, "daysBefore", 7),
                Rules.rule(AlertType.MAINTENANCE_OVERDUE)));
    when(maintenance.detect(any(), any(), any())).thenReturn(List.of());
    when(reconciler.reconcile(anySet(), anyList()))
        .thenReturn(new AlertReconciler.Reconciliation(0, 0));

    evaluator.evaluate();

    verify(consumption, never()).detect(any(), any(), any());
  }

  @Test
  void evaluate_whenSourceFails_skipsItsTypesAndKeepsEvaluatingTheOthers() {
    Detection overdue = detection(AlertType.MAINTENANCE_OVERDUE, "MAINTENANCE_OVERDUE:1");
    when(maintenance.detect(any(), any(), any())).thenReturn(List.of(overdue));
    when(consumption.detect(any(), any(), any())).thenThrow(FeignFailures.unavailable());
    when(reconciler.reconcile(anySet(), anyList()))
        .thenReturn(new AlertReconciler.Reconciliation(1, 0));

    EvaluationResponse response = evaluator.evaluate();

    assertThat(response.skipped()).containsExactly(AlertType.HIGH_CONSUMPTION);
    assertThat(response.created()).isEqualTo(1);
    verify(reconciler).reconcile(evaluated.capture(), detections.capture());
    assertThat(evaluated.getValue())
        .containsExactlyInAnyOrder(AlertType.MAINTENANCE_DUE, AlertType.MAINTENANCE_OVERDUE);
    assertThat(detections.getValue()).containsExactly(overdue);
  }

  @Test
  void evaluate_whenTheCircuitBreakerStopsOneSource_skipsItsTypes() {
    when(maintenance.detect(any(), any(), any())).thenReturn(List.of());
    when(consumption.detect(any(), any(), any())).thenThrow(FeignFailures.circuitBroken());
    when(reconciler.reconcile(anySet(), anyList()))
        .thenReturn(new AlertReconciler.Reconciliation(0, 0));

    EvaluationResponse response = evaluator.evaluate();

    assertThat(response.skipped()).containsExactly(AlertType.HIGH_CONSUMPTION);
    verify(reconciler).reconcile(evaluated.capture(), detections.capture());
    assertThat(evaluated.getValue())
        .containsExactlyInAnyOrder(AlertType.MAINTENANCE_DUE, AlertType.MAINTENANCE_OVERDUE);
  }

  @Test
  void evaluate_whenEverySourceFails_skipsEveryTypeInOrder() {
    when(maintenance.detect(any(), any(), any())).thenThrow(FeignFailures.unavailable());
    when(consumption.detect(any(), any(), any())).thenThrow(FeignFailures.unavailable());
    when(reconciler.reconcile(anySet(), anyList()))
        .thenReturn(new AlertReconciler.Reconciliation(0, 0));

    EvaluationResponse response = evaluator.evaluate();

    assertThat(response.skipped())
        .containsExactly(
            AlertType.MAINTENANCE_DUE, AlertType.MAINTENANCE_OVERDUE, AlertType.HIGH_CONSUMPTION);
    verify(reconciler).reconcile(evaluated.capture(), detections.capture());
    assertThat(evaluated.getValue()).isEmpty();
  }

  private static AlertType[] allTypes() {
    return new AlertType[] {
      AlertType.MAINTENANCE_DUE, AlertType.MAINTENANCE_OVERDUE, AlertType.HIGH_CONSUMPTION
    };
  }

  private static List<AlertRule> rules() {
    return Arrays.asList(
        Rules.rule(AlertType.MAINTENANCE_DUE, "daysBefore", 7),
        Rules.rule(AlertType.MAINTENANCE_OVERDUE),
        Rules.rule(AlertType.HIGH_CONSUMPTION, "thresholdPercent", 25));
  }

  private static AlertRule disabled(AlertType type) {
    AlertRule rule = Rules.rule(type);
    rule.setEnabled(false);
    return rule;
  }

  private static Detection detection(AlertType type, String key) {
    return new Detection(type, AlertSeverity.WARNING, UUID.randomUUID(), null, "message", key);
  }
}
