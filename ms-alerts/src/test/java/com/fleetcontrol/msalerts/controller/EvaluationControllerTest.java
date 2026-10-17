package com.fleetcontrol.msalerts.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fleetcontrol.msalerts.models.dto.internal.DriversPageDto;
import com.fleetcontrol.msalerts.models.dto.internal.FuelConsumptionDto;
import com.fleetcontrol.msalerts.models.dto.internal.MaintenanceOrdersDto;
import com.fleetcontrol.msalerts.models.dto.internal.VehicleDto;
import com.fleetcontrol.msalerts.models.entities.Alert;
import com.fleetcontrol.msalerts.models.entities.AlertRule;
import com.fleetcontrol.msalerts.models.enums.AlertSeverity;
import com.fleetcontrol.msalerts.models.enums.AlertStatus;
import com.fleetcontrol.msalerts.models.enums.AlertType;
import com.fleetcontrol.msalerts.support.AbstractIntegrationTest;
import com.fleetcontrol.msalerts.support.FeignFailures;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.ResultActions;

/** Integration tests of the evaluator, launched through its endpoint with mocked sources. */
class EvaluationControllerTest extends AbstractIntegrationTest {

  private static final String EVALUATE = "/api/alerts/evaluate";
  private static final String PENDING = "PENDING";
  private static final int PAGE_SIZE = 100;

  private final LocalDate today = LocalDate.now(ZoneOffset.UTC);
  private final UUID overdueVehicle = UUID.randomUUID();
  private final UUID dueVehicle = UUID.randomUUID();
  private final UUID expiringDriver = UUID.randomUUID();
  private final UUID expiredDriver = UUID.randomUUID();
  private final UUID thirstyVehicle = UUID.randomUUID();

  private MaintenanceOrdersDto maintenanceOrders;

  @BeforeEach
  void stubSources() {
    maintenanceOrders =
        new MaintenanceOrdersDto(
            List.of(
                new MaintenanceOrdersDto.Order(
                    UUID.randomUUID(), overdueVehicle, "OIL_CHANGE", today.minusDays(7)),
                new MaintenanceOrdersDto.Order(
                    UUID.randomUUID(), dueVehicle, "TIRES", today.plusDays(3))),
            1);
    when(maintenanceClient.orders(anyString(), anyString(), anyInt(), anyInt()))
        .thenReturn(maintenanceOrders);
    when(driversClient.expiringLicenses(anyInt(), anyInt(), anyInt()))
        .thenReturn(
            new DriversPageDto(
                List.of(
                    new DriversPageDto.Driver(
                        expiringDriver, "Miguel Torres", today.plusDays(10)),
                    new DriversPageDto.Driver(expiredDriver, "Ana Ruiz", today.minusDays(3))),
                1));
    when(fuelClient.consumption(today.minusDays(30).toString(), today.toString()))
        .thenReturn(consumption("1111-AAA", "14.0"));
    when(fuelClient.consumption(today.minusDays(90).toString(), today.minusDays(31).toString()))
        .thenReturn(consumption("1111-AAA", "10.0"));
    when(vehiclesClient.getVehicle(any()))
        .thenAnswer(invocation -> new VehicleDto(invocation.getArgument(0), "3342-HJK"));
  }

  @Test
  void evaluate_createsAnAlertForEachCondition() throws Exception {
    evaluate()
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.created").value(5))
        .andExpect(jsonPath("$.resolved").value(0))
        .andExpect(jsonPath("$.skipped").isEmpty())
        .andExpect(jsonPath("$.durationMs").exists());

    List<Alert> alerts = alertRepository.findAll();
    assertThat(alerts)
        .extracting(Alert::getType)
        .containsExactlyInAnyOrder(
            AlertType.MAINTENANCE_OVERDUE,
            AlertType.MAINTENANCE_DUE,
            AlertType.LICENSE_EXPIRING,
            AlertType.LICENSE_EXPIRED,
            AlertType.HIGH_CONSUMPTION);
    assertThat(alerts).allMatch(alert -> alert.getStatus() == AlertStatus.OPEN);
    assertThat(alert(AlertType.MAINTENANCE_OVERDUE).getSeverity())
        .isEqualTo(AlertSeverity.CRITICAL);
    assertThat(alert(AlertType.MAINTENANCE_OVERDUE).getMessage())
        .isEqualTo(
            "Mantenimiento OIL_CHANGE vencido desde el "
                + today.minusDays(7)
                + " en el vehículo 3342-HJK");
    assertThat(alert(AlertType.MAINTENANCE_OVERDUE).getVehicleId()).isEqualTo(overdueVehicle);
    assertThat(alert(AlertType.MAINTENANCE_DUE).getSeverity()).isEqualTo(AlertSeverity.WARNING);
    assertThat(alert(AlertType.LICENSE_EXPIRING).getMessage())
        .isEqualTo("La licencia de Miguel Torres caduca el " + today.plusDays(10));
    assertThat(alert(AlertType.LICENSE_EXPIRING).getDriverId()).isEqualTo(expiringDriver);
    assertThat(alert(AlertType.LICENSE_EXPIRED).getSeverity()).isEqualTo(AlertSeverity.CRITICAL);
    assertThat(alert(AlertType.LICENSE_EXPIRED).getDriverId()).isEqualTo(expiredDriver);
    assertThat(alert(AlertType.HIGH_CONSUMPTION).getMessage()).contains("1111-AAA", "40%");
    assertThat(alert(AlertType.HIGH_CONSUMPTION).getVehicleId()).isEqualTo(thirstyVehicle);
  }

  @Test
  void evaluate_twice_doesNotDuplicateTheAlerts() throws Exception {
    evaluate().andExpect(jsonPath("$.created").value(5));

    evaluate()
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.created").value(0))
        .andExpect(jsonPath("$.resolved").value(0));

    assertThat(alertRepository.count()).isEqualTo(5);
  }

  @Test
  void evaluate_resolvesTheAlertsWhoseConditionDisappears() throws Exception {
    evaluate();
    when(maintenanceClient.orders(anyString(), anyString(), anyInt(), anyInt()))
        .thenReturn(new MaintenanceOrdersDto(List.of(), 1));

    evaluate()
        .andExpect(jsonPath("$.created").value(0))
        .andExpect(jsonPath("$.resolved").value(2));

    assertThat(alert(AlertType.MAINTENANCE_OVERDUE).getStatus()).isEqualTo(AlertStatus.RESOLVED);
    assertThat(alert(AlertType.MAINTENANCE_OVERDUE).getResolvedAt()).isNotNull();
    assertThat(alert(AlertType.MAINTENANCE_DUE).getStatus()).isEqualTo(AlertStatus.RESOLVED);
    assertThat(alert(AlertType.LICENSE_EXPIRING).getStatus()).isEqualTo(AlertStatus.OPEN);
  }

  @Test
  void evaluate_createsNewAlertWhenResolvedConditionReappears() throws Exception {
    evaluate();
    when(maintenanceClient.orders(anyString(), anyString(), anyInt(), anyInt()))
        .thenReturn(new MaintenanceOrdersDto(List.of(), 1));
    evaluate();
    when(maintenanceClient.orders(anyString(), anyString(), anyInt(), anyInt()))
        .thenReturn(maintenanceOrders);

    evaluate().andExpect(jsonPath("$.created").value(2));

    assertThat(alertRepository.findAll())
        .filteredOn(alert -> alert.getType() == AlertType.MAINTENANCE_OVERDUE)
        .extracting(Alert::getStatus)
        .containsExactlyInAnyOrder(AlertStatus.RESOLVED, AlertStatus.OPEN);
  }

  @Test
  void evaluate_keepsAnAcknowledgedAlertWhileTheConditionHolds() throws Exception {
    evaluate();
    Alert overdue = alert(AlertType.MAINTENANCE_OVERDUE);
    overdue.setStatus(AlertStatus.ACKNOWLEDGED);
    alertRepository.saveAndFlush(overdue);

    evaluate()
        .andExpect(jsonPath("$.created").value(0))
        .andExpect(jsonPath("$.resolved").value(0));

    assertThat(alert(AlertType.MAINTENANCE_OVERDUE).getStatus())
        .isEqualTo(AlertStatus.ACKNOWLEDGED);
  }

  @Test
  void evaluate_skipsTheTypesWhoseSourceFailsAndKeepsTheirAlerts() throws Exception {
    Alert existing =
        saveAlert(
            AlertType.HIGH_CONSUMPTION,
            AlertSeverity.WARNING,
            AlertStatus.OPEN,
            Instant.now(),
            UUID.randomUUID());
    when(fuelClient.consumption(anyString(), anyString())).thenThrow(FeignFailures.unavailable());

    evaluate()
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.created").value(4))
        .andExpect(jsonPath("$.resolved").value(0))
        .andExpect(jsonPath("$.skipped.length()").value(1))
        .andExpect(jsonPath("$.skipped[0]").value("HIGH_CONSUMPTION"));

    assertThat(alertRepository.findById(existing.getId()).orElseThrow().getStatus())
        .isEqualTo(AlertStatus.OPEN);
  }

  @Test
  void evaluate_withEverySourceDown_skipsEveryType() throws Exception {
    when(maintenanceClient.orders(anyString(), anyString(), anyInt(), anyInt()))
        .thenThrow(FeignFailures.unavailable());
    when(driversClient.expiringLicenses(anyInt(), anyInt(), anyInt()))
        .thenThrow(FeignFailures.unavailable());
    when(fuelClient.consumption(anyString(), anyString())).thenThrow(FeignFailures.unavailable());

    evaluate()
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.created").value(0))
        .andExpect(jsonPath("$.skipped.length()").value(5))
        .andExpect(jsonPath("$.skipped[0]").value("MAINTENANCE_DUE"))
        .andExpect(jsonPath("$.skipped[4]").value("HIGH_CONSUMPTION"));
  }

  @Test
  void evaluate_usesTheIdWhenThePlateIsNotAvailable() throws Exception {
    doThrow(FeignFailures.unavailable()).when(vehiclesClient).getVehicle(any());

    evaluate().andExpect(jsonPath("$.created").value(5));

    assertThat(alert(AlertType.MAINTENANCE_OVERDUE).getMessage())
        .endsWith(overdueVehicle.toString());
  }

  @Test
  void evaluate_resolvesAlertsOfDisabledRuleWithoutCallingSource() throws Exception {
    saveAlert(AlertType.LICENSE_EXPIRED, AlertSeverity.CRITICAL, AlertStatus.OPEN, Instant.now());
    disable(AlertType.LICENSE_EXPIRING);
    disable(AlertType.LICENSE_EXPIRED);

    evaluate()
        .andExpect(jsonPath("$.created").value(3))
        .andExpect(jsonPath("$.resolved").value(1));

    verifyNoInteractions(driversClient);
    assertThat(alertRepository.findAll())
        .filteredOn(alert -> alert.getType() == AlertType.LICENSE_EXPIRED)
        .extracting(Alert::getStatus)
        .containsExactly(AlertStatus.RESOLVED);
  }

  @Test
  void evaluate_usesTheConfiguredDays() throws Exception {
    AlertRule rule = ruleRepository.findById(AlertType.MAINTENANCE_DUE).orElseThrow();
    rule.setParams(new HashMap<>(Map.of("daysBefore", 14)));
    ruleRepository.saveAndFlush(rule);

    evaluate();

    verify(maintenanceClient).orders(PENDING, today.plusDays(14).toString(), 0, PAGE_SIZE);
    verify(driversClient).expiringLicenses(30, 0, PAGE_SIZE);
  }

  @Test
  void evaluate_withOnlyOverdueEnabled_asksForTheOrdersBeforeToday() throws Exception {
    disable(AlertType.MAINTENANCE_DUE);

    evaluate().andExpect(jsonPath("$.resolved").value(0));

    verify(maintenanceClient)
        .orders(eq(PENDING), eq(today.minusDays(1).toString()), eq(0), eq(PAGE_SIZE));
    assertThat(alertRepository.findAll())
        .extracting(Alert::getType)
        .doesNotContain(AlertType.MAINTENANCE_DUE);
  }

  // --------------------------------------------------------------- helpers

  private ResultActions evaluate() throws Exception {
    return mockMvc.perform(post(EVALUATE).header(HttpHeaders.AUTHORIZATION, bearer("ADMIN")));
  }

  private Alert alert(AlertType type) {
    return alertRepository.findAll().stream()
        .filter(alert -> alert.getType() == type)
        .findFirst()
        .orElseThrow();
  }

  private void disable(AlertType type) {
    AlertRule rule = ruleRepository.findById(type).orElseThrow();
    rule.setEnabled(false);
    ruleRepository.saveAndFlush(rule);
  }

  private FuelConsumptionDto consumption(String plate, String average) {
    return new FuelConsumptionDto(
        List.of(
            new FuelConsumptionDto.VehicleConsumption(
                thirstyVehicle, plate, new BigDecimal(average))));
  }
}
