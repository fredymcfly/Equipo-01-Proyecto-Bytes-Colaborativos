package com.fleetcontrol.msmaintenance.service;

import com.fleetcontrol.msmaintenance.client.VehicleGateway;
import com.fleetcontrol.msmaintenance.constants.BusinessConstants;
import com.fleetcontrol.msmaintenance.exception.ApiException;
import com.fleetcontrol.msmaintenance.models.entities.MaintenanceOrder;
import com.fleetcontrol.msmaintenance.models.entities.MaintenancePlan;
import com.fleetcontrol.msmaintenance.models.enums.MaintenanceOrderStatus;
import com.fleetcontrol.msmaintenance.repository.MaintenanceOrderRepository;
import com.fleetcontrol.msmaintenance.repository.MaintenancePlanRepository;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/** Creates the pending orders of the plans that are about to fall due. */
@Service
@RequiredArgsConstructor
@Slf4j
public class MaintenanceOrderGenerator {

  private final MaintenancePlanRepository planRepository;
  private final MaintenanceOrderRepository orderRepository;
  private final VehicleGateway vehicleGateway;

  /**
   * Creates a pending order for every active plan without an open order that is due within 7 days
   * or 500 km. Plans whose vehicle cannot be read are skipped and tried again the next day.
   *
   * @return the number of orders created
   */
  public int generate(LocalDate today) {
    int created = 0;
    for (MaintenancePlan plan : planRepository.findActiveWithoutOpenOrder()) {
      LocalDate scheduledFor = scheduledDate(plan, today);
      if (scheduledFor != null && createOrder(plan, scheduledFor)) {
        created++;
      }
    }
    return created;
  }

  /** Date for the new order, or null when the plan is not due yet. */
  private LocalDate scheduledDate(MaintenancePlan plan, LocalDate today) {
    if (plan.getNextDueAt() != null
        && !plan.getNextDueAt().isAfter(today.plusDays(BusinessConstants.DAYS_AHEAD))) {
      return plan.getNextDueAt();
    }
    if (plan.getNextDueKm() != null && isCloseInKm(plan)) {
      return today;
    }
    return null;
  }

  private boolean isCloseInKm(MaintenancePlan plan) {
    try {
      Integer odometerKm = vehicleGateway.getVehicle(plan.getVehicleId()).odometerKm();
      return odometerKm != null && plan.getNextDueKm() - odometerKm <= BusinessConstants.KM_AHEAD;
    } catch (ApiException exception) {
      log.warn(
          "Could not read the odometer of vehicle {}: {}",
          plan.getVehicleId(),
          exception.getMessage());
      return false;
    }
  }

  private boolean createOrder(MaintenancePlan plan, LocalDate scheduledFor) {
    MaintenanceOrder order = new MaintenanceOrder();
    order.setPlanId(plan.getId());
    order.setVehicleId(plan.getVehicleId());
    order.setType(plan.getType());
    order.setStatus(MaintenanceOrderStatus.PENDING);
    order.setScheduledFor(scheduledFor);
    try {
      orderRepository.saveAndFlush(order);
      return true;
    } catch (DataIntegrityViolationException exception) {
      log.debug("Plan {} already has an open order", plan.getId());
      return false;
    }
  }
}
