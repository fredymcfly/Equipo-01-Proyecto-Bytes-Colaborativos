package com.fleetcontrol.msalerts.service.detector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fleetcontrol.msalerts.client.DriversClient;
import com.fleetcontrol.msalerts.models.dto.internal.Detection;
import com.fleetcontrol.msalerts.models.dto.internal.DriversPageDto;
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

/** Unit tests of the detection of licenses that expire or have expired. */
@ExtendWith(MockitoExtension.class)
class LicenseDetectorTest {

  private static final LocalDate TODAY = LocalDate.of(2026, 10, 5);
  private static final Function<UUID, String> PLATES = vehicleId -> "unused";

  @Mock private DriversClient client;

  private LicenseDetector detector;

  @BeforeEach
  void setUp() {
    detector = new LicenseDetector(client);
  }

  @Test
  void types_areTheTwoLicenseTypes() {
    assertThat(detector.types())
        .containsExactlyInAnyOrder(AlertType.LICENSE_EXPIRING, AlertType.LICENSE_EXPIRED);
  }

  @Test
  void detect_classifiesLicensesAsExpiringOrExpired() {
    UUID expiringId = UUID.randomUUID();
    UUID expiredId = UUID.randomUUID();
    when(client.expiringLicenses(30, 0, 100))
        .thenReturn(
            page(
                1,
                driver(expiringId, "Miguel Torres", LocalDate.of(2026, 10, 20)),
                driver(expiredId, "Ana Ruiz", LocalDate.of(2026, 10, 2))));

    List<Detection> detections = detector.detect(bothRules(), TODAY, PLATES);

    assertThat(detections)
        .containsExactly(
            new Detection(
                AlertType.LICENSE_EXPIRING,
                AlertSeverity.WARNING,
                null,
                expiringId,
                "La licencia de Miguel Torres caduca el 2026-10-20",
                "LICENSE_EXPIRING:" + expiringId),
            new Detection(
                AlertType.LICENSE_EXPIRED,
                AlertSeverity.CRITICAL,
                null,
                expiredId,
                "La licencia de Ana Ruiz caducó el 2026-10-02",
                "LICENSE_EXPIRED:" + expiredId));
  }

  @Test
  void detect_aLicenseThatExpiresTodayIsStillExpiring() {
    when(client.expiringLicenses(30, 0, 100))
        .thenReturn(page(1, driver(UUID.randomUUID(), "Ana Ruiz", TODAY)));

    List<Detection> detections = detector.detect(bothRules(), TODAY, PLATES);

    assertThat(detections).extracting(Detection::type).containsExactly(AlertType.LICENSE_EXPIRING);
  }

  @Test
  void detect_usesTheConfiguredDays() {
    when(client.expiringLicenses(45, 0, 100)).thenReturn(page(1));
    Map<AlertType, AlertRule> rules =
        Map.of(
            AlertType.LICENSE_EXPIRING,
            Rules.rule(AlertType.LICENSE_EXPIRING, "daysBefore", 45));

    assertThat(detector.detect(rules, TODAY, PLATES)).isEmpty();

    verify(client).expiringLicenses(45, 0, 100);
  }

  @Test
  void detect_withOnlyTheExpiredRule_asksForZeroDaysAndIgnoresTheOthers() {
    UUID expiredId = UUID.randomUUID();
    when(client.expiringLicenses(0, 0, 100))
        .thenReturn(
            page(
                1,
                driver(expiredId, "Ana Ruiz", TODAY.minusDays(1)),
                driver(UUID.randomUUID(), "Luis Gil", TODAY)));
    Map<AlertType, AlertRule> rules =
        Map.of(AlertType.LICENSE_EXPIRED, Rules.rule(AlertType.LICENSE_EXPIRED));

    List<Detection> detections = detector.detect(rules, TODAY, PLATES);

    assertThat(detections).extracting(Detection::driverId).containsExactly(expiredId);
  }

  @Test
  void detect_withOnlyTheExpiringRule_ignoresTheExpiredLicenses() {
    when(client.expiringLicenses(30, 0, 100))
        .thenReturn(
            page(
                1,
                driver(UUID.randomUUID(), "Ana Ruiz", TODAY.minusDays(1)),
                driver(UUID.randomUUID(), "Luis Gil", TODAY.plusDays(5))));
    Map<AlertType, AlertRule> rules =
        Map.of(
            AlertType.LICENSE_EXPIRING,
            Rules.rule(AlertType.LICENSE_EXPIRING, "daysBefore", 30));

    List<Detection> detections = detector.detect(rules, TODAY, PLATES);

    assertThat(detections).hasSize(1);
    assertThat(detections.get(0).type()).isEqualTo(AlertType.LICENSE_EXPIRING);
  }

  @Test
  void detect_readsEveryPage() {
    when(client.expiringLicenses(30, 0, 100))
        .thenReturn(page(2, driver(UUID.randomUUID(), "Ana Ruiz", TODAY.plusDays(1))));
    when(client.expiringLicenses(30, 1, 100))
        .thenReturn(page(2, driver(UUID.randomUUID(), "Luis Gil", TODAY.plusDays(2))));

    assertThat(detector.detect(bothRules(), TODAY, PLATES)).hasSize(2);
  }

  @Test
  void detect_ignoresDriversWithoutDateAndNullAnswers() {
    when(client.expiringLicenses(30, 0, 100))
        .thenReturn(page(1, driver(UUID.randomUUID(), "Ana Ruiz", null)));
    assertThat(detector.detect(bothRules(), TODAY, PLATES)).isEmpty();

    when(client.expiringLicenses(30, 0, 100)).thenReturn(null);
    assertThat(detector.detect(bothRules(), TODAY, PLATES)).isEmpty();
  }

  @Test
  void detect_whenTheServiceFails_letsTheErrorPropagate() {
    when(client.expiringLicenses(30, 0, 100)).thenThrow(FeignFailures.unavailable());

    assertThatThrownBy(() -> detector.detect(bothRules(), TODAY, PLATES))
        .isInstanceOf(FeignException.class);
  }

  private Map<AlertType, AlertRule> bothRules() {
    return Map.of(
        AlertType.LICENSE_EXPIRING,
        Rules.rule(AlertType.LICENSE_EXPIRING, "daysBefore", 30),
        AlertType.LICENSE_EXPIRED,
        Rules.rule(AlertType.LICENSE_EXPIRED));
  }

  private static DriversPageDto.Driver driver(UUID id, String name, LocalDate expiresAt) {
    return new DriversPageDto.Driver(id, name, expiresAt);
  }

  private static DriversPageDto page(int totalPages, DriversPageDto.Driver... drivers) {
    return new DriversPageDto(List.of(drivers), totalPages);
  }
}
