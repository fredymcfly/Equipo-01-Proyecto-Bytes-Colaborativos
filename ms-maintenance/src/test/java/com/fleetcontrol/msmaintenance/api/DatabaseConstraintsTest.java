package com.fleetcontrol.msmaintenance.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fleetcontrol.msmaintenance.models.entities.MaintenancePlan;
import com.fleetcontrol.msmaintenance.models.enums.MaintenanceOrderStatus;
import com.fleetcontrol.msmaintenance.models.enums.MaintenanceType;
import com.fleetcontrol.msmaintenance.support.AbstractIntegrationTest;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

/** Rules enforced by the PostgreSQL schema (partial unique indexes) and the plan query. */
class DatabaseConstraintsTest extends AbstractIntegrationTest {

  private final LocalDate today = LocalDate.of(2026, 10, 13);
  private final UUID vehicleId = UUID.randomUUID();

  @Test
  void plans_twoActivePlansOfSameVehicleAndType_areRejected() {
    savePlan(vehicleId, MaintenanceType.OIL_CHANGE, today);

    assertThatThrownBy(() -> savePlan(vehicleId, MaintenanceType.OIL_CHANGE, today))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void plans_newPlanIsAllowedOnceThePreviousOneIsInactive() {
    MaintenancePlan first = savePlan(vehicleId, MaintenanceType.OIL_CHANGE, today);
    first.setActive(false);
    planRepository.saveAndFlush(first);

    assertThatCode(() -> savePlan(vehicleId, MaintenanceType.OIL_CHANGE, today))
        .doesNotThrowAnyException();
  }

  @Test
  void plans_sameTypeOnDifferentVehicles_isAllowed() {
    savePlan(vehicleId, MaintenanceType.OIL_CHANGE, today);

    assertThatCode(() -> savePlan(UUID.randomUUID(), MaintenanceType.OIL_CHANGE, today))
        .doesNotThrowAnyException();
  }

  @Test
  void orders_twoOpenOrdersOfSamePlan_areRejected() {
    MaintenancePlan plan = savePlan(vehicleId, MaintenanceType.OIL_CHANGE, today);
    saveOrder(plan, MaintenanceOrderStatus.PENDING, today);

    assertThatThrownBy(() -> saveOrder(plan, MaintenanceOrderStatus.IN_PROGRESS, today))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void orders_newOpenOrderIsAllowedAfterTheOtherIsCompleted() {
    MaintenancePlan plan = savePlan(vehicleId, MaintenanceType.OIL_CHANGE, today);
    saveOrder(plan, MaintenanceOrderStatus.COMPLETED, today.minusDays(90));
    saveOrder(plan, MaintenanceOrderStatus.COMPLETED, today.minusDays(180));

    assertThatCode(() -> saveOrder(plan, MaintenanceOrderStatus.PENDING, today))
        .doesNotThrowAnyException();
  }

  private MaintenancePlan planWithOrder(MaintenanceOrderStatus status, LocalDate scheduledFor) {
    MaintenancePlan plan = savePlan(UUID.randomUUID(), MaintenanceType.OIL_CHANGE, today);
    saveOrder(plan, status, scheduledFor);
    return plan;
  }

  @Test
  void findActiveWithoutOpenOrder_returnsOnlyActivePlansWithoutOpenOrders() {
    planWithOrder(MaintenanceOrderStatus.PENDING, today);
    planWithOrder(MaintenanceOrderStatus.IN_PROGRESS, today);
    MaintenancePlan inactive = savePlan(UUID.randomUUID(), MaintenanceType.OIL_CHANGE, today);
    inactive.setActive(false);
    planRepository.saveAndFlush(inactive);
    MaintenancePlan withOnlyCompletedOrder =
        planWithOrder(MaintenanceOrderStatus.COMPLETED, today.minusDays(90));
    MaintenancePlan withoutOrders = savePlan(UUID.randomUUID(), MaintenanceType.OIL_CHANGE, today);

    assertThat(planRepository.findActiveWithoutOpenOrder())
        .extracting(MaintenancePlan::getId)
        .containsExactlyInAnyOrder(withoutOrders.getId(), withOnlyCompletedOrder.getId());
  }
}
