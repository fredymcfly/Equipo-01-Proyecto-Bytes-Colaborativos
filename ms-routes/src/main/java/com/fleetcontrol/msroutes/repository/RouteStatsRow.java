package com.fleetcontrol.msroutes.repository;

import java.math.BigDecimal;

/** One row of the statistics query: the completed routes of a period. */
public interface RouteStatsRow {

  /** First day of the period, as {@code YYYY-MM-DD}. */
  String getPeriod();

  Long getRoutes();

  BigDecimal getDistanceKm();

  Double getAvgDurationMin();
}
