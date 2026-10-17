package com.fleetcontrol.msalerts.service.detector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fleetcontrol.msalerts.client.MaintenanceClient;
import com.fleetcontrol.msalerts.models.dto.internal.Detection;
import com.fleetcontrol.msalerts.models.dto.internal.MaintenanceOrdersDto;
import com.fleetcontrol.msalerts.models.entities.AlertRule;
import com.fleetcontrol.msalerts.models.enums.AlertSeverity;
import com.fleetcontrol.msalerts.models.enums.AlertType;
import com.fleetcontrol.msalerts.support.FeignFailures;
import com.fleetcontrol.msalerts.support.Rules;
import feign.FeignException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests of the detection of late and close maintenance orders. */
@ExtendWith(MockitoExtension.class)
class MaintenanceDetectorTest {

  private static final LocalDate TODAY = LocalDate.of(2026, 10, 5);
  private static final String PENDING = "PENDING";
  private static final Function<UUID, String> PLATES = vehicleId -> "3342-HJK";

  @Mock private MaintenanceClient client;

  private MaintenanceDetector detector;

  @BeforeEach
  void setUp() {
    detector = new MaintenanceDetector(client);
  }

  @Test
  void types_areTheTwoMaintenanceTypes() {
    assertThat(detector.types())
        .containsExactlyInAnyOrder(AlertType.MAINTENANCE_DUE, AlertType.MAINTENANCE_OVERDUE);
  }

  @Test
  void detect_classifiesOrdersAsOverdueOrDue() {
    UUID overdueOrder = UUID.randomUUID();
    UUID dueOrder = UUID.randomUUID();
    UUID vehicleId = UUID.randomUUID();
    when(client.orders(PENDING, "2026-10-12", 0, 100))
        .thenReturn(
            page(
                1,
                order(overdueOrder, vehicleId, "OIL_CHANGE", LocalDate.of(2026, 9, 28)),
                order(dueOrder, vehicleId, "TIRES", LocalDate.of(2026, 10, 8))));

    List<Detection> detections = detector.detect(bothRules(), TODAY, PLATES);

    assertThat(detections)
        .containsExactly(
            new Detection(
                AlertType.MAINTENANCE_OVERDUE,
                AlertSeverity.CRITICAL,
                vehicleId,
                null,
                "Mantenimiento OIL_CHANGE vencido desde el 2026-09-28 en el vehículo 3342-HJK",
                "MAINTENANCE_OVERDUE:" + overdueOrder),
            new Detection(
                AlertType.MAINTENANCE_DUE,
                AlertSeverity.WARNING,
                vehicleId,
                null,
                "Mantenimiento TIRES programado para el 2026-10-08 en el vehículo 3342-HJK",
                "MAINTENANCE_DUE:" + dueOrder));
  }

  @Test
  void detect_anOrderScheduledTodayIsDueNotOverdue() {
    when(client.orders(PENDING, "2026-10-12", 0, 100))
        .thenReturn(page(1, order(UUID.randomUUID(), UUID.randomUUID(), "OIL_CHANGE", TODAY)));

    List<Detection> detections = detector.detect(bothRules(), TODAY, PLATES);

    assertThat(detections).extracting(Detection::type).containsExactly(AlertType.MAINTENANCE_DUE);
  }

  @Test
  void detect_usesTheSeverityOfEachRule() {
    when(client.orders(PENDING, "2026-10-12", 0, 100))
        .thenReturn(
            page(
                1,
                order(UUID.randomUUID(), UUID.randomUUID(), "OIL_CHANGE", TODAY.minusDays(1)),
                order(UUID.randomUUID(), UUID.randomUUID(), "TIRES", TODAY.plusDays(1))));
    Map<AlertType, AlertRule> rules =
        Map.of(
            AlertType.MAINTENANCE_DUE,
            Rules.rule(AlertType.MAINTENANCE_DUE, AlertSeverity.INFO, Map.of("daysBefore", 7)),
            AlertType.MAINTENANCE_OVERDUE,
            Rules.rule(AlertType.MAINTENANCE_OVERDUE, AlertSeverity.WARNING, Map.of()));

    List<Detection> detections = detector.detect(rules, TODAY, PLATES);

    assertThat(detections)
        .extracting(Detection::type, Detection::severity)
        .containsExactly(
            tuple(AlertType.MAINTENANCE_OVERDUE, AlertSeverity.WARNING),
            tuple(AlertType.MAINTENANCE_DUE, AlertSeverity.INFO));
  }

  @Test
  void detect_readsEveryPage() {
    UUID first = UUID.randomUUID();
    UUID second = UUID.randomUUID();
    when(client.orders(PENDING, "2026-10-12", 0, 100))
        .thenReturn(page(2, order(first, UUID.randomUUID(), "OIL_CHANGE", TODAY.minusDays(2))));
    when(client.orders(PENDING, "2026-10-12", 1, 100))
        .thenReturn(page(2, order(second, UUID.randomUUID(), "TIRES", TODAY.minusDays(1))));

    List<Detection> detections = detector.detect(bothRules(), TODAY, PLATES);

    assertThat(detections)
        .extracting(Detection::dedupKey)
        .containsExactly("MAINTENANCE_OVERDUE:" + first, "MAINTENANCE_OVERDUE:" + second);
    verify(client).orders(PENDING, "2026-10-12", 1, 100);
  }

  @Test
  void detect_withOnlyTheOverdueRule_asksForTheOrdersBeforeToday() {
    when(client.orders(PENDING, "2026-10-04", 0, 100))
        .thenReturn(
            page(
                1,
                order(UUID.randomUUID(), UUID.randomUUID(), "OIL_CHANGE", TODAY.minusDays(1)),
                order(UUID.randomUUID(), UUID.randomUUID(), "TIRES", TODAY.plusDays(2))));
    Map<AlertType, AlertRule> rules =
        Map.of(
            AlertType.MAINTENANCE_OVERDUE, Rules.rule(AlertType.MAINTENANCE_OVERDUE));

    List<Detection> detections = detector.detect(rules, TODAY, PLATES);

    assertThat(detections)
        .extracting(Detection::type)
        .containsExactly(AlertType.MAINTENANCE_OVERDUE);
  }

  @Test
  void detect_withOnlyTheDueRule_ignoresTheLateOrders() {
    when(client.orders(PENDING, "2026-10-19", 0, 100))
        .thenReturn(
            page(
                1,
                order(UUID.randomUUID(), UUID.randomUUID(), "OIL_CHANGE", TODAY.minusDays(1)),
                order(UUID.randomUUID(), UUID.randomUUID(), "TIRES", TODAY.plusDays(14))));
    Map<AlertType, AlertRule> rules =
        Map.of(
            AlertType.MAINTENANCE_DUE,
            Rules.rule(AlertType.MAINTENANCE_DUE, "daysBefore", 14));

    List<Detection> detections = detector.detect(rules, TODAY, PLATES);

    assertThat(detections).extracting(Detection::type).containsExactly(AlertType.MAINTENANCE_DUE);
  }

  @Test
  void detect_ignoresOrdersWithoutDateAndEmptyAnswers() {
    when(client.orders(PENDING, "2026-10-12", 0, 100))
        .thenReturn(page(1, order(UUID.randomUUID(), UUID.randomUUID(), "OIL_CHANGE", null)));

    assertThat(detector.detect(bothRules(), TODAY, PLATES)).isEmpty();
  }

  @Test
  void detect_withNullAnswer_findsNothing() {
    when(client.orders(PENDING, "2026-10-12", 0, 100)).thenReturn(null);

    assertThat(detector.detect(bothRules(), TODAY, PLATES)).isEmpty();
  }

  @Test
  void detect_whenTheServiceFails_letsTheErrorPropagate() {
    when(client.orders(PENDING, "2026-10-12", 0, 100)).thenThrow(FeignFailures.unavailable());

    assertThatThrownBy(() -> detector.detect(bothRules(), TODAY, PLATES))
        .isInstanceOf(FeignException.class);
  }

  private Map<AlertType, AlertRule> bothRules() {
    return Map.of(
        AlertType.MAINTENANCE_DUE,
        Rules.rule(AlertType.MAINTENANCE_DUE, "daysBefore", 7),
        AlertType.MAINTENANCE_OVERDUE,
        Rules.rule(AlertType.MAINTENANCE_OVERDUE));
  }

  private static MaintenanceOrdersDto.Order order(
      UUID id, UUID vehicleId, String type, LocalDate scheduledFor) {
    return new MaintenanceOrdersDto.Order(id, vehicleId, type, scheduledFor);
  }

  private static MaintenanceOrdersDto page(int totalPages, MaintenanceOrdersDto.Order... orders) {
    return new MaintenanceOrdersDto(List.of(orders), totalPages);
  }
}
