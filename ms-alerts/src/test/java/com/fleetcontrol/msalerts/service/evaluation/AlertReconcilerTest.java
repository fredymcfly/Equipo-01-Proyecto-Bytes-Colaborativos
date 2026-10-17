package com.fleetcontrol.msalerts.service.evaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fleetcontrol.msalerts.models.dto.internal.Detection;
import com.fleetcontrol.msalerts.models.entities.Alert;
import com.fleetcontrol.msalerts.models.enums.AlertSeverity;
import com.fleetcontrol.msalerts.models.enums.AlertStatus;
import com.fleetcontrol.msalerts.models.enums.AlertType;
import com.fleetcontrol.msalerts.repository.AlertRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests of the update of the stored alerts from the conditions found. */
@ExtendWith(MockitoExtension.class)
class AlertReconcilerTest {

  private static final Instant NOW = Instant.parse("2026-10-05T10:30:00Z");
  private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
  private static final List<AlertStatus> ACTIVE =
      List.of(AlertStatus.OPEN, AlertStatus.ACKNOWLEDGED);
  private static final Set<AlertType> MAINTENANCE =
      EnumSet.of(AlertType.MAINTENANCE_DUE, AlertType.MAINTENANCE_OVERDUE);

  @Mock private AlertRepository repository;
  @Captor private ArgumentCaptor<List<Alert>> saved;

  private AlertReconciler reconciler;

  @BeforeEach
  void setUp() {
    reconciler = new AlertReconciler(repository, CLOCK);
  }

  @Test
  void reconcile_createsOpenAlertForNewCondition() {
    UUID vehicleId = UUID.randomUUID();
    when(repository.findByTypeInAndStatusIn(MAINTENANCE, ACTIVE)).thenReturn(List.of());

    AlertReconciler.Reconciliation result =
        reconciler.reconcile(MAINTENANCE, List.of(detection("MAINTENANCE_OVERDUE:1", vehicleId)));

    assertThat(result).isEqualTo(new AlertReconciler.Reconciliation(1, 0));
    verify(repository, times(2)).saveAll(saved.capture());
    assertThat(saved.getAllValues().get(0)).hasSize(1);
    Alert alert = saved.getAllValues().get(0).get(0);
    assertThat(alert.getType()).isEqualTo(AlertType.MAINTENANCE_OVERDUE);
    assertThat(alert.getSeverity()).isEqualTo(AlertSeverity.CRITICAL);
    assertThat(alert.getStatus()).isEqualTo(AlertStatus.OPEN);
    assertThat(alert.getVehicleId()).isEqualTo(vehicleId);
    assertThat(alert.getMessage()).isEqualTo("message");
    assertThat(alert.getDedupKey()).isEqualTo("MAINTENANCE_OVERDUE:1");
    assertThat(alert.getCreatedAt()).isEqualTo(NOW);
  }

  @Test
  void reconcile_doesNotDuplicateAnActiveAlert() {
    when(repository.findByTypeInAndStatusIn(MAINTENANCE, ACTIVE))
        .thenReturn(
            List.of(
                alert("MAINTENANCE_OVERDUE:1", AlertStatus.OPEN),
                alert("MAINTENANCE_DUE:2", AlertStatus.ACKNOWLEDGED)));

    AlertReconciler.Reconciliation result =
        reconciler.reconcile(
            MAINTENANCE,
            List.of(
                detection("MAINTENANCE_OVERDUE:1", UUID.randomUUID()),
                detection("MAINTENANCE_DUE:2", UUID.randomUUID())));

    assertThat(result).isEqualTo(new AlertReconciler.Reconciliation(0, 0));
    verify(repository, times(2)).saveAll(saved.capture());
    assertThat(saved.getAllValues()).allMatch(List::isEmpty);
  }

  @Test
  void reconcile_resolvesTheActiveAlertsThatAreNoLongerDetected() {
    Alert open = alert("MAINTENANCE_OVERDUE:1", AlertStatus.OPEN);
    Alert acknowledged = alert("MAINTENANCE_DUE:2", AlertStatus.ACKNOWLEDGED);
    when(repository.findByTypeInAndStatusIn(MAINTENANCE, ACTIVE))
        .thenReturn(List.of(open, acknowledged));

    AlertReconciler.Reconciliation result = reconciler.reconcile(MAINTENANCE, List.of());

    assertThat(result).isEqualTo(new AlertReconciler.Reconciliation(0, 2));
    assertThat(open.getStatus()).isEqualTo(AlertStatus.RESOLVED);
    assertThat(open.getResolvedAt()).isEqualTo(NOW);
    assertThat(acknowledged.getStatus()).isEqualTo(AlertStatus.RESOLVED);
    assertThat(acknowledged.getResolvedAt()).isEqualTo(NOW);
  }

  @Test
  void reconcile_createsAndResolvesInTheSameRun() {
    Alert gone = alert("MAINTENANCE_OVERDUE:1", AlertStatus.OPEN);
    when(repository.findByTypeInAndStatusIn(MAINTENANCE, ACTIVE)).thenReturn(List.of(gone));

    AlertReconciler.Reconciliation result =
        reconciler.reconcile(
            MAINTENANCE, List.of(detection("MAINTENANCE_DUE:2", UUID.randomUUID())));

    assertThat(result).isEqualTo(new AlertReconciler.Reconciliation(1, 1));
    assertThat(gone.getStatus()).isEqualTo(AlertStatus.RESOLVED);
  }

  @Test
  void reconcile_countsRepeatedDetectionOnce() {
    when(repository.findByTypeInAndStatusIn(MAINTENANCE, ACTIVE)).thenReturn(List.of());

    AlertReconciler.Reconciliation result =
        reconciler.reconcile(
            MAINTENANCE,
            List.of(
                detection("MAINTENANCE_OVERDUE:1", UUID.randomUUID()),
                detection("MAINTENANCE_OVERDUE:1", UUID.randomUUID())));

    assertThat(result.created()).isEqualTo(1);
  }

  @Test
  void reconcile_withoutEvaluatedTypes_doesNothing() {
    AlertReconciler.Reconciliation result =
        reconciler.reconcile(EnumSet.noneOf(AlertType.class), List.of());

    assertThat(result).isEqualTo(new AlertReconciler.Reconciliation(0, 0));
    verifyNoInteractions(repository);
  }

  private static Detection detection(String key, UUID vehicleId) {
    return new Detection(
        AlertType.MAINTENANCE_OVERDUE, AlertSeverity.CRITICAL, vehicleId, null, "message", key);
  }

  private static Alert alert(String key, AlertStatus status) {
    Alert alert = new Alert();
    alert.setId(UUID.randomUUID());
    alert.setType(AlertType.MAINTENANCE_OVERDUE);
    alert.setSeverity(AlertSeverity.CRITICAL);
    alert.setStatus(status);
    alert.setMessage("message");
    alert.setDedupKey(key);
    alert.setCreatedAt(NOW.minusSeconds(3600));
    return alert;
  }
}
