package com.fleetcontrol.msmaintenance.models.dto.response;

import com.fleetcontrol.msmaintenance.models.enums.Granularity;
import com.fleetcontrol.msmaintenance.models.enums.MaintenanceType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Time series of completed orders, with a breakdown by type and the upcoming workload. */
public record StatsResponse(
    LocalDate from,
    LocalDate to,
    Granularity granularity,
    Totals totals,
    List<SeriesPoint> series,
    List<TypeStats> byType,
    Upcoming upcoming) {

  /** Orders and cost of the whole range. */
  public record Totals(long orders, BigDecimal cost) {}

  /** Orders and cost of one period, identified by its first day. */
  public record SeriesPoint(LocalDate period, long orders, BigDecimal cost) {}

  /** Orders and cost of one maintenance type. */
  public record TypeStats(MaintenanceType type, long orders, BigDecimal cost) {}

  /** Pending orders that are already late or fall due within seven days. */
  public record Upcoming(long overdue, long dueIn7Days) {}
}
