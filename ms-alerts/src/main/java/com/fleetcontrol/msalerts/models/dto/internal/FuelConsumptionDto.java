package com.fleetcontrol.msalerts.models.dto.internal;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Answer of {@code GET /api/fuel/consumption} in ms-fuel, with the fields the evaluator uses. */
public record FuelConsumptionDto(List<VehicleConsumption> vehicles) {

  /** Consumption of one vehicle in the period. */
  public record VehicleConsumption(UUID vehicleId, String plate, BigDecimal avgL100km) {}
}
