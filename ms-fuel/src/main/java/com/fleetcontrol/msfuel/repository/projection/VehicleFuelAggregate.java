package com.fleetcontrol.msfuel.repository.projection;

import java.math.BigDecimal;
import java.util.UUID;

/** Agregado de los repostajes de un vehículo dentro de un periodo. */
public record VehicleFuelAggregate(
    UUID vehicleId,
    BigDecimal liters,
    BigDecimal cost,
    BigDecimal avgConsumptionL100km,
    Integer minOdometerKm,
    Integer maxOdometerKm) {

  /** Construye el agregado a partir de la fila de la consulta nativa. */
  public static VehicleFuelAggregate fromRow(Object[] row) {
    return new VehicleFuelAggregate(
        (UUID) row[0],
        (BigDecimal) row[1],
        (BigDecimal) row[2],
        (BigDecimal) row[3],
        (Integer) row[4],
        (Integer) row[5]);
  }
}
