package com.fleetcontrol.msalerts.service.detector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.fleetcontrol.msalerts.client.FuelClient;
import com.fleetcontrol.msalerts.models.dto.internal.Detection;
import com.fleetcontrol.msalerts.models.dto.internal.FuelConsumptionDto;
import com.fleetcontrol.msalerts.models.entities.AlertRule;
import com.fleetcontrol.msalerts.models.enums.AlertSeverity;
import com.fleetcontrol.msalerts.models.enums.AlertType;
import com.fleetcontrol.msalerts.support.FeignFailures;
import com.fleetcontrol.msalerts.support.Rules;
import feign.FeignException;
import java.math.BigDecimal;
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

/** Unit tests of the detection of vehicles whose consumption has risen. */
@ExtendWith(MockitoExtension.class)
class ConsumptionDetectorTest {

  private static final LocalDate TODAY = LocalDate.of(2026, 10, 5);
  private static final String RECENT_FROM = "2026-09-05";
  private static final String RECENT_TO = "2026-10-05";
  private static final String PREVIOUS_FROM = "2026-07-07";
  private static final String PREVIOUS_TO = "2026-09-04";
  private static final UUID VEHICLE = UUID.randomUUID();

  @Mock private FuelClient client;

  private ConsumptionDetector detector;

  @BeforeEach
  void setUp() {
    detector = new ConsumptionDetector(client);
  }

  @Test
  void types_isHighConsumption() {
    assertThat(detector.types()).containsExactly(AlertType.HIGH_CONSUMPTION);
  }

  @Test
  void detect_comparesTheLast30DaysWithThe60DaysBefore() {
    stub("1111-AAA", "14.0", "10.0");

    List<Detection> detections = detector.detect(rules(25), TODAY, vehicleId -> "unused");

    assertThat(detections)
        .containsExactly(
            new Detection(
                AlertType.HIGH_CONSUMPTION,
                AlertSeverity.WARNING,
                VEHICLE,
                null,
                "El consumo del vehículo 1111-AAA subió un 40% "
                    + "(14.0 L/100 km frente a 10.0 L/100 km)",
                "HIGH_CONSUMPTION:" + VEHICLE));
  }

  @Test
  void detect_exactlyAtTheThreshold_isNotEnough() {
    stub("1111-AAA", "12.5", "10.0");

    assertThat(detector.detect(rules(25), TODAY, vehicleId -> "unused")).isEmpty();
  }

  @Test
  void detect_justAboveTheThreshold_raisesTheAlert() {
    stub("1111-AAA", "12.6", "10.0");

    assertThat(detector.detect(rules(25), TODAY, vehicleId -> "unused")).hasSize(1);
  }

  @Test
  void detect_usesTheConfiguredThreshold() {
    stub("1111-AAA", "11.5", "10.0");

    assertThat(detector.detect(rules(25), TODAY, vehicleId -> "unused")).isEmpty();
    assertThat(detector.detect(rules(10), TODAY, vehicleId -> "unused")).hasSize(1);
  }

  @Test
  void detect_aLowerConsumption_raisesNothing() {
    stub("1111-AAA", "8.0", "10.0");

    assertThat(detector.detect(rules(25), TODAY, vehicleId -> "unused")).isEmpty();
  }

  @Test
  void detect_withoutPreviousData_raisesNothing() {
    when(client.consumption(RECENT_FROM, RECENT_TO)).thenReturn(consumption("1111-AAA", "14.0"));
    when(client.consumption(PREVIOUS_FROM, PREVIOUS_TO))
        .thenReturn(new FuelConsumptionDto(List.of()));

    assertThat(detector.detect(rules(25), TODAY, vehicleId -> "unused")).isEmpty();
  }

  @Test
  void detect_withPreviousConsumptionOfZero_raisesNothing() {
    stub("1111-AAA", "14.0", "0");

    assertThat(detector.detect(rules(25), TODAY, vehicleId -> "unused")).isEmpty();
  }

  @Test
  void detect_withoutRecentConsumption_raisesNothing() {
    when(client.consumption(RECENT_FROM, RECENT_TO))
        .thenReturn(
            new FuelConsumptionDto(
                List.of(new FuelConsumptionDto.VehicleConsumption(VEHICLE, "1111-AAA", null))));
    when(client.consumption(PREVIOUS_FROM, PREVIOUS_TO))
        .thenReturn(consumption("1111-AAA", "10.0"));

    assertThat(detector.detect(rules(25), TODAY, vehicleId -> "unused")).isEmpty();
  }

  @Test
  void detect_withoutPlateInFuel_asksForIt() {
    stub(null, "14.0", "10.0");
    Function<UUID, String> plates = vehicleId -> "9999-ZZZ";

    List<Detection> detections = detector.detect(rules(25), TODAY, plates);

    assertThat(detections.get(0).message()).contains("9999-ZZZ");
  }

  @Test
  void detect_withNullAnswers_findsNothing() {
    when(client.consumption(RECENT_FROM, RECENT_TO)).thenReturn(null);
    when(client.consumption(PREVIOUS_FROM, PREVIOUS_TO)).thenReturn(null);

    assertThat(detector.detect(rules(25), TODAY, vehicleId -> "unused")).isEmpty();
  }

  @Test
  void detect_whenFuelFails_letsTheErrorPropagate() {
    when(client.consumption(RECENT_FROM, RECENT_TO)).thenThrow(FeignFailures.unavailable());

    assertThatThrownBy(() -> detector.detect(rules(25), TODAY, vehicleId -> "unused"))
        .isInstanceOf(FeignException.class);
  }

  private Map<AlertType, AlertRule> rules(int threshold) {
    return Map.of(
        AlertType.HIGH_CONSUMPTION,
        Rules.rule(AlertType.HIGH_CONSUMPTION, "thresholdPercent", threshold));
  }

  private void stub(String plate, String recent, String previous) {
    when(client.consumption(RECENT_FROM, RECENT_TO)).thenReturn(consumption(plate, recent));
    when(client.consumption(PREVIOUS_FROM, PREVIOUS_TO)).thenReturn(consumption(plate, previous));
  }

  private static FuelConsumptionDto consumption(String plate, String average) {
    return new FuelConsumptionDto(
        List.of(
            new FuelConsumptionDto.VehicleConsumption(VEHICLE, plate, new BigDecimal(average))));
  }
}
