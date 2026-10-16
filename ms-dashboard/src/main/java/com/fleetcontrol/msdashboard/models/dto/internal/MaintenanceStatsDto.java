package com.fleetcontrol.msdashboard.models.dto.internal;

import java.math.BigDecimal;
import java.util.List;

/** Answer of {@code GET /api/maintenance/stats} in ms-maintenance. */
public record MaintenanceStatsDto(Totals totals, List<Point> series) {

  /** Totals of the whole range. */
  public record Totals(long orders, BigDecimal cost) {}

  /** Values of one period. */
  public record Point(String period, long orders, BigDecimal cost) {}
}
