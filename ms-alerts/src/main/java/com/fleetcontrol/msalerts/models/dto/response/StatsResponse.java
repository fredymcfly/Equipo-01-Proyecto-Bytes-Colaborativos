package com.fleetcontrol.msalerts.models.dto.response;

import com.fleetcontrol.msalerts.models.enums.AlertSeverity;
import com.fleetcontrol.msalerts.models.enums.Granularity;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Alerts created over time and alerts that are open now.
 *
 * @param totals alerts created in the whole range
 * @param series one point per period, including the empty ones
 * @param open open alerts by severity
 */
public record StatsResponse(
    LocalDate from,
    LocalDate to,
    Granularity granularity,
    Totals totals,
    List<Point> series,
    Map<AlertSeverity, Long> open) {

  /** Alerts created in the whole range. */
  public record Totals(long created) {}

  /** Alerts created in one period, with the period starting on the given day (UTC). */
  public record Point(LocalDate period, long created, Map<AlertSeverity, Long> bySeverity) {}
}
