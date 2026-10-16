package com.fleetcontrol.msdashboard.models.dto.internal;

import java.util.List;
import java.util.Map;

/** Answer of {@code GET /api/alerts/stats} in ms-alerts. */
public record AlertsStatsDto(Totals totals, List<Point> series, Map<String, Long> open) {

  /** Totals of the whole range. */
  public record Totals(long created) {}

  /** Values of one period. */
  public record Point(String period, long created) {}
}
