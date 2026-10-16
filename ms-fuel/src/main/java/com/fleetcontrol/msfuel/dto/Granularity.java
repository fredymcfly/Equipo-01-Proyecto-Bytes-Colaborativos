package com.fleetcontrol.msfuel.dto;

/** Tamaño del periodo de una serie temporal. */
public enum Granularity {
  DAY("day"),
  WEEK("week"),
  MONTH("month");

  private final String unit;

  Granularity(String unit) {
    this.unit = unit;
  }

  /** Unidad de truncado de fecha que espera PostgreSQL. */
  public String unit() {
    return unit;
  }
}
