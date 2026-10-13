package com.fleetcontrol.msfuel.repository.projection;

import java.math.BigDecimal;

/** Totales de los repostajes de todo un periodo. */
public record FuelTotalsAggregate(
    BigDecimal liters,
    BigDecimal cost,
    BigDecimal avgPricePerLiter,
    BigDecimal avgConsumptionL100km) {

  /** Construye los totales a partir de la fila de la consulta nativa. */
  public static FuelTotalsAggregate fromRow(Object[] row) {
    return new FuelTotalsAggregate(
        (BigDecimal) row[0], (BigDecimal) row[1], (BigDecimal) row[2], (BigDecimal) row[3]);
  }
}
