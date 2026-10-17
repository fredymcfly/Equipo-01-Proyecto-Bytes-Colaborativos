package com.fleetcontrol.msalerts.service.evaluation;

import com.fleetcontrol.msalerts.models.dto.internal.Detection;
import com.fleetcontrol.msalerts.models.dto.response.EvaluationResponse;
import com.fleetcontrol.msalerts.models.entities.AlertRule;
import com.fleetcontrol.msalerts.models.enums.AlertType;
import com.fleetcontrol.msalerts.repository.AlertRuleRepository;
import com.fleetcontrol.msalerts.service.detector.AlertDetector;
import com.fleetcontrol.msalerts.service.detector.PlateResolver;
import feign.FeignException;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.circuitbreaker.NoFallbackAvailableException;
import org.springframework.stereotype.Service;

/**
 * Runs the rules against the source services and updates the alerts. A type whose source service
 * does not answer, or whose circuit breaker is open, is skipped, so its alerts are neither created nor resolved on that round. A
 * type whose rule is disabled is evaluated with no conditions, so its active alerts are resolved.
 *
 * <p>It has no transaction: the calls to the source services can be slow, so the database work is
 * done by {@link AlertReconciler}. Runs are serialized, so a manual run and the scheduled one never
 * overlap.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertEvaluator {

  private final AlertRuleRepository ruleRepository;
  private final List<AlertDetector> detectors;
  private final PlateResolver plateResolver;
  private final AlertReconciler reconciler;
  private final Clock clock;

  private final ReentrantLock lock = new ReentrantLock();

  /** Evaluates every rule once and returns what changed. */
  public EvaluationResponse evaluate() {
    lock.lock();
    try {
      return run();
    } finally {
      lock.unlock();
    }
  }

  private EvaluationResponse run() {
    long start = System.nanoTime();
    Map<AlertType, AlertRule> rules = new EnumMap<>(AlertType.class);
    ruleRepository.findAll().forEach(rule -> rules.put(rule.getType(), rule));
    LocalDate today = LocalDate.now(clock);
    Function<UUID, String> plates = plateResolver.newLookup();

    Set<AlertType> evaluated = EnumSet.noneOf(AlertType.class);
    Set<AlertType> skipped = EnumSet.noneOf(AlertType.class);
    List<Detection> detections = new ArrayList<>();
    for (AlertDetector detector : detectors) {
      Map<AlertType, AlertRule> enabled = enabledRules(detector.types(), rules);
      try {
        if (!enabled.isEmpty()) {
          detections.addAll(detector.detect(enabled, today, plates));
        }
        evaluated.addAll(detector.types());
      } catch (FeignException | NoFallbackAvailableException exception) {
        log.warn("Skipping {}: a source service failed", detector.types(), exception);
        skipped.addAll(detector.types());
      }
    }

    AlertReconciler.Reconciliation result = reconciler.reconcile(evaluated, detections);
    long durationMs = Duration.ofNanos(System.nanoTime() - start).toMillis();
    log.info(
        "Evaluation finished: {} created, {} resolved, {} skipped, {} ms",
        result.created(),
        result.resolved(),
        skipped,
        durationMs);
    return new EvaluationResponse(
        result.created(), result.resolved(), List.copyOf(skipped), durationMs);
  }

  private Map<AlertType, AlertRule> enabledRules(
      Set<AlertType> types, Map<AlertType, AlertRule> rules) {
    Map<AlertType, AlertRule> enabled = new EnumMap<>(AlertType.class);
    for (AlertType type : types) {
      AlertRule rule = rules.get(type);
      if (rule != null && rule.isEnabled()) {
        enabled.put(type, rule);
      }
    }
    return enabled;
  }
}
