package com.fleetcontrol.msdashboard.models.dto.internal;

import java.math.BigDecimal;
import java.util.List;

/** Answer of {@code GET /api/routes/stats} in ms-routes. */
public record RoutesStatsDto(Totals totals, List<Point> series) {

  /** Totals of the whole range. */
  public record Totals(long routes, BigDecimal distanceKm) {}

  /** Values of one period. */
  public record Point(String period, long routes, BigDecimal distanceKm) {}
}
