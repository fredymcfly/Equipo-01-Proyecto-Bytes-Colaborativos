package com.fleetcontrol.msroutes.enums;

/** Size of the periods in which the route statistics are grouped. */
public enum Granularity {
  DAY("day"),
  WEEK("week"),
  MONTH("month");

  private final String truncUnit;

  Granularity(String truncUnit) {
    this.truncUnit = truncUnit;
  }

  /** Returns the unit understood by the PostgreSQL function {@code date_trunc}. */
  public String truncUnit() {
    return truncUnit;
  }
}
