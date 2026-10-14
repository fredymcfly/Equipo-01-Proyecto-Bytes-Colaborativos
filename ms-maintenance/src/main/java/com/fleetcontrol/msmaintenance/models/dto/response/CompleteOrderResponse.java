package com.fleetcontrol.msmaintenance.models.dto.response;

import com.fleetcontrol.msmaintenance.models.enums.MaintenanceOrderStatus;
import com.fleetcontrol.msmaintenance.models.enums.MaintenanceType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Result of completing an order, with the new due dates of its plan. */
public record CompleteOrderResponse(
    UUID id,
    UUID vehicleId,
    MaintenanceType type,
    MaintenanceOrderStatus status,
    Instant completedAt,
    BigDecimal cost,
    Integer odometerKm,
    String workshop,
    PlanSummary plan) {

  /** The plan after recalculating its next due date and odometer. */
  public record PlanSummary(UUID id, LocalDate nextDueAt, Integer nextDueKm) {}
}
