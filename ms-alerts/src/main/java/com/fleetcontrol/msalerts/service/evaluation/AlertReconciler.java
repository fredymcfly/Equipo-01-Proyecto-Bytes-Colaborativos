package com.fleetcontrol.msalerts.service.evaluation;

import com.fleetcontrol.msalerts.models.dto.internal.Detection;
import com.fleetcontrol.msalerts.models.entities.Alert;
import com.fleetcontrol.msalerts.models.enums.AlertStatus;
import com.fleetcontrol.msalerts.models.enums.AlertType;
import com.fleetcontrol.msalerts.repository.AlertRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Brings the stored alerts in line with the conditions found. It is a separate bean so that its
 * transaction only covers the database work, never the calls to the source services.
 */
@Service
@RequiredArgsConstructor
public class AlertReconciler {

  private static final List<AlertStatus> ACTIVE =
      List.of(AlertStatus.OPEN, AlertStatus.ACKNOWLEDGED);

  private final AlertRepository repository;
  private final Clock clock;

  /** Number of alerts created and resolved by a reconciliation. */
  public record Reconciliation(int created, int resolved) {}

  /**
   * Creates an alert for each condition that has no active alert, and resolves the active alerts
   * whose condition no longer holds. Only the alerts of the evaluated types are touched.
   */
  @Transactional
  public Reconciliation reconcile(Set<AlertType> evaluated, List<Detection> detections) {
    if (evaluated.isEmpty()) {
      return new Reconciliation(0, 0);
    }
    Instant now = Instant.now(clock);
    List<Alert> active = repository.findByTypeInAndStatusIn(evaluated, ACTIVE);
    Set<String> activeKeys = new HashSet<>();
    active.forEach(alert -> activeKeys.add(alert.getDedupKey()));
    Map<String, Detection> detected = new LinkedHashMap<>();
    detections.forEach(detection -> detected.putIfAbsent(detection.dedupKey(), detection));

    List<Alert> toCreate =
        detected.values().stream()
            .filter(detection -> !activeKeys.contains(detection.dedupKey()))
            .map(detection -> newAlert(detection, now))
            .toList();
    List<Alert> toResolve =
        active.stream().filter(alert -> !detected.containsKey(alert.getDedupKey())).toList();
    toResolve.forEach(
        alert -> {
          alert.setStatus(AlertStatus.RESOLVED);
          alert.setResolvedAt(now);
        });
    repository.saveAll(toCreate);
    repository.saveAll(toResolve);
    return new Reconciliation(toCreate.size(), toResolve.size());
  }

  private Alert newAlert(Detection detection, Instant now) {
    Alert alert = new Alert();
    alert.setType(detection.type());
    alert.setSeverity(detection.severity());
    alert.setStatus(AlertStatus.OPEN);
    alert.setVehicleId(detection.vehicleId());
    alert.setDriverId(detection.driverId());
    alert.setMessage(detection.message());
    alert.setDedupKey(detection.dedupKey());
    alert.setCreatedAt(now);
    return alert;
  }
}
