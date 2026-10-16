package com.fleetcontrol.msmaintenance.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fleetcontrol.msmaintenance.models.dto.internal.VehicleResponse;
import com.fleetcontrol.msmaintenance.models.entities.MaintenanceOrder;
import com.fleetcontrol.msmaintenance.models.entities.MaintenancePlan;
import com.fleetcontrol.msmaintenance.models.enums.MaintenanceOrderStatus;
import com.fleetcontrol.msmaintenance.models.enums.MaintenanceType;
import com.fleetcontrol.msmaintenance.service.MaintenanceOrderGenerator;
import com.fleetcontrol.msmaintenance.support.AbstractIntegrationTest;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/** Whole life of an order on a real PostgreSQL: generated, started, completed and counted. */
class OrderFlowTest extends AbstractIntegrationTest {

  private static final String ORDERS = "/api/maintenance/orders";
  private static final String COMPLETE_BODY =
      """
      {"cost": 120.5, "odometerKm": 10200, "workshop": "Taller Centro", "notes": "Cambio de aceite"}
      """;

  @Autowired private MaintenanceOrderGenerator generator;

  private final LocalDate today = LocalDate.now(ZoneOffset.UTC);
  private final UUID vehicleId = UUID.randomUUID();

  private ResultActions postJson(String url, String body) throws Exception {
    return mockMvc.perform(
        post(url)
            .header(HttpHeaders.AUTHORIZATION, bearer("MANAGER"))
            .contentType(MediaType.APPLICATION_JSON)
            .content(body));
  }

  private ResultActions getJson(String url) throws Exception {
    return mockMvc.perform(get(url).header(HttpHeaders.AUTHORIZATION, bearer("MANAGER")));
  }

  /** Plan that falls due in three days, so the generator creates its order. */
  private MaintenancePlan planDueInThreeDays() {
    return savePlan(vehicleId, MaintenanceType.OIL_CHANGE, today.minusDays(87));
  }

  private UUID onlyOrderId() {
    return orderRepository.findAll().get(0).getId();
  }

  @Test
  void generator_createsOneOrderPerPlanAndIsIdempotent() {
    MaintenancePlan plan = planDueInThreeDays();

    assertThat(generator.generate(today)).isEqualTo(1);
    assertThat(generator.generate(today)).isZero();

    MaintenanceOrder order = orderRepository.findAll().get(0);
    assertThat(order.getPlanId()).isEqualTo(plan.getId());
    assertThat(order.getStatus()).isEqualTo(MaintenanceOrderStatus.PENDING);
    assertThat(order.getScheduledFor()).isEqualTo(today.plusDays(3));
  }

  @Test
  void generator_planNotDueYet_createsNothing() {
    savePlan(vehicleId, MaintenanceType.OIL_CHANGE, today.minusDays(10));
    when(vehicleGateway.getVehicle(vehicleId))
        .thenReturn(new VehicleResponse(vehicleId, "AVAILABLE", 10_000));

    assertThat(generator.generate(today)).isZero();
    assertThat(orderRepository.count()).isZero();
  }

  @Test
  void fullFlow_startThenComplete_updatesOrderPlanVehicleAndStats() throws Exception {
    planDueInThreeDays();
    generator.generate(today);
    UUID orderId = onlyOrderId();
    when(vehicleGateway.getVehicle(vehicleId))
        .thenReturn(
            new VehicleResponse(vehicleId, "AVAILABLE", 10_000),
            new VehicleResponse(vehicleId, "IN_MAINTENANCE", 10_000));

    postJson(ORDERS + "/" + orderId + "/start", "")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
        .andExpect(jsonPath("$.startedAt").exists());
    verify(vehicleGateway).changeStatus(vehicleId, "IN_MAINTENANCE", null);

    postJson(ORDERS + "/" + orderId + "/start", "")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error").value("INVALID_ORDER_STATE"));

    postJson(ORDERS + "/" + orderId + "/complete", COMPLETE_BODY)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("COMPLETED"))
        .andExpect(jsonPath("$.odometerKm").value(10_200))
        .andExpect(jsonPath("$.cost").value(120.5))
        .andExpect(jsonPath("$.plan.nextDueKm").value(20_200))
        .andExpect(jsonPath("$.plan.nextDueAt").value(today.plusDays(90).toString()));
    verify(vehicleGateway).changeStatus(vehicleId, "AVAILABLE", 10_200);

    MaintenanceOrder order = orderRepository.findById(orderId).orElseThrow();
    assertThat(order.getStatus()).isEqualTo(MaintenanceOrderStatus.COMPLETED);
    assertThat(order.getCost()).isEqualByComparingTo("120.50");
    MaintenancePlan updated = planRepository.findAll().get(0);
    assertThat(updated.getLastDoneKm()).isEqualTo(10_200);
    assertThat(updated.getLastDoneAt()).isEqualTo(today);

    // The plan has no open order any more, so it can generate the next one.
    assertThat(generator.generate(today)).isZero();

    getJson("/api/maintenance/stats?from=" + today + "&to=" + today + "&granularity=DAY")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totals.orders").value(1))
        .andExpect(jsonPath("$.totals.cost").value(120.5))
        .andExpect(jsonPath("$.series.length()").value(1))
        .andExpect(jsonPath("$.byType[0].type").value("OIL_CHANGE"));
  }

  @Test
  void start_vehicleInUse_returnsConflictAndKeepsOrderPending() throws Exception {
    planDueInThreeDays();
    generator.generate(today);
    UUID orderId = onlyOrderId();
    when(vehicleGateway.getVehicle(vehicleId))
        .thenReturn(new VehicleResponse(vehicleId, "IN_USE", 10_000));

    postJson(ORDERS + "/" + orderId + "/start", "")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error").value("VEHICLE_NOT_AVAILABLE"));

    assertThat(orderRepository.findById(orderId).orElseThrow().getStatus())
        .isEqualTo(MaintenanceOrderStatus.PENDING);
    verify(vehicleGateway, never()).changeStatus(vehicleId, "IN_MAINTENANCE", null);
  }

  @Test
  void complete_odometerLowerThanVehicle_returnsConflict() throws Exception {
    MaintenancePlan plan = planDueInThreeDays();
    MaintenanceOrder order = saveOrder(plan, MaintenanceOrderStatus.IN_PROGRESS, today.plusDays(3));
    when(vehicleGateway.getVehicle(vehicleId))
        .thenReturn(new VehicleResponse(vehicleId, "IN_MAINTENANCE", 15_000));

    postJson(ORDERS + "/" + order.getId() + "/complete", COMPLETE_BODY)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error").value("INVALID_ODOMETER"));

    assertThat(orderRepository.findById(order.getId()).orElseThrow().getStatus())
        .isEqualTo(MaintenanceOrderStatus.IN_PROGRESS);
  }

  @Test
  void complete_missingCost_returnsValidationError() throws Exception {
    postJson(ORDERS + "/" + UUID.randomUUID() + "/complete", "{}")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
  }

  @Test
  void start_unknownOrder_returnsNotFound() throws Exception {
    postJson(ORDERS + "/" + UUID.randomUUID() + "/start", "")
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("ORDER_NOT_FOUND"));
  }

  @Test
  void list_filtersByStatusAndDueDate() throws Exception {
    MaintenancePlan first = savePlan(UUID.randomUUID(), MaintenanceType.OIL_CHANGE, today);
    MaintenancePlan second = savePlan(UUID.randomUUID(), MaintenanceType.BRAKE_CHECK, today);
    saveOrder(first, MaintenanceOrderStatus.PENDING, today.plusDays(2));
    saveOrder(second, MaintenanceOrderStatus.PENDING, today.plusDays(20));
    saveOrder(first, MaintenanceOrderStatus.COMPLETED, today.minusDays(60));

    getJson(ORDERS + "?status=PENDING")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(2))
        .andExpect(jsonPath("$.content[0].scheduledFor").value(today.plusDays(2).toString()));

    getJson(ORDERS + "?status=PENDING&dueBefore=" + today.plusDays(7))
        .andExpect(jsonPath("$.totalElements").value(1));

    getJson(ORDERS + "?status=COMPLETED&type=OIL_CHANGE")
        .andExpect(jsonPath("$.totalElements").value(1));

    getJson(ORDERS + "?type=BRAKE_CHECK").andExpect(jsonPath("$.totalElements").value(1));
  }

  @Test
  void stats_countsOverdueAndUpcomingPendingOrders() throws Exception {
    MaintenancePlan first = savePlan(UUID.randomUUID(), MaintenanceType.OIL_CHANGE, today);
    MaintenancePlan second = savePlan(UUID.randomUUID(), MaintenanceType.BRAKE_CHECK, today);
    MaintenancePlan third = savePlan(UUID.randomUUID(), MaintenanceType.TIRE_ROTATION, today);
    saveOrder(first, MaintenanceOrderStatus.PENDING, today.minusDays(2));
    saveOrder(second, MaintenanceOrderStatus.PENDING, today.plusDays(7));
    saveOrder(third, MaintenanceOrderStatus.PENDING, today.plusDays(8));

    getJson("/api/maintenance/stats?from=" + today.minusDays(30) + "&to=" + today)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.granularity").value("DAY"))
        .andExpect(jsonPath("$.series.length()").value(31))
        .andExpect(jsonPath("$.upcoming.overdue").value(1))
        .andExpect(jsonPath("$.upcoming.dueIn7Days").value(1));
  }

  @Test
  void stats_invalidGranularityOrRange_returnValidationError() throws Exception {
    getJson("/api/maintenance/stats?from=" + today + "&to=" + today + "&granularity=day")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));

    getJson("/api/maintenance/stats?from=" + today + "&to=" + today.minusDays(1))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
  }
}
