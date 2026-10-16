package com.fleetcontrol.msdashboard.models.dto.internal;

import java.math.BigDecimal;
import java.util.List;

/** Answer of {@code GET /api/fuel/stats} in ms-fuel. */
public record FuelStatsDto(Totals totals, List<Point> series) {

  /** Totals of the whole range. */
  public record Totals(BigDecimal liters, BigDecimal cost, BigDecimal avgL100km) {}

  /** Values of one period. */
  public record Point(String period, BigDecimal liters, BigDecimal cost, BigDecimal avgL100km) {}
}
