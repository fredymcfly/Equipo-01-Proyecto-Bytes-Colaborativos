package com.fleetcontrol.msroutes.dto;

import com.fleetcontrol.msroutes.enums.Granularity;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Time series of the completed routes.
 *
 * @param from first day of the range
 * @param to last day of the range
 * @param granularity size of each period of the series
 * @param totals totals of the whole range
 * @param series one point per period that has completed routes
 */
@Schema(description = "Completed routes grouped by their end date")
public record RouteStatsResponse(
    @Schema(example = "2026-09-07") LocalDate from,
    @Schema(example = "2026-09-27") LocalDate to,
    @Schema(example = "WEEK") Granularity granularity,
    Totals totals,
    List<Point> series) {

  /**
   * Totals of the whole range.
   *
   * @param routes number of completed routes
   * @param distanceKm sum of the actual distances
   */
  public record Totals(
      @Schema(example = "302") long routes, @Schema(example = "28084.9") BigDecimal distanceKm) {}

  /**
   * Values of one period.
   *
   * @param period first day of the period
   * @param routes number of completed routes
   * @param distanceKm sum of the actual distances
   * @param avgDurationMin average duration in minutes
   */
  public record Point(
      @Schema(example = "2026-09-07") String period,
      @Schema(example = "97") long routes,
      @Schema(example = "9034.1") BigDecimal distanceKm,
      @Schema(example = "128") long avgDurationMin) {}
}
