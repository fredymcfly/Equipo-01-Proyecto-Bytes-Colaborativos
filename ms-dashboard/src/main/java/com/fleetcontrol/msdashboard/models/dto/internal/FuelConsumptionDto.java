package com.fleetcontrol.msdashboard.models.dto.internal;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Answer of {@code GET /api/fuel/consumption} in ms-fuel. */
public record FuelConsumptionDto(List<VehicleConsumption> vehicles) {

  /** Consumption of one vehicle in the period. */
  public record VehicleConsumption(
      UUID vehicleId,
      String plate,
      String type,
      BigDecimal liters,
      BigDecimal cost,
      BigDecimal distanceKm,
      BigDecimal avgL100km) {}
}
