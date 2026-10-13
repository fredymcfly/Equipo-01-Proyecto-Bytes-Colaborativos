package com.fleetcontrol.msfuel.repository.projection;

import java.math.BigDecimal;

/** Agregado de los repostajes de un periodo de la serie temporal. */
public record FuelPeriodAggregate(
    String period,
    BigDecimal liters,
    BigDecimal cost,
    BigDecimal avgPricePerLiter,
    BigDecimal avgConsumptionL100km) {

  /** Construye el agregado a partir de la fila de la consulta nativa. */
  public static FuelPeriodAggregate fromRow(Object[] row) {
    return new FuelPeriodAggregate(
        (String) row[0],
        (BigDecimal) row[1],
        (BigDecimal) row[2],
        (BigDecimal) row[3],
        (BigDecimal) row[4]);
  }
}
