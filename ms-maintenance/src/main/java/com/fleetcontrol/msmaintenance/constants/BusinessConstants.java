package com.fleetcontrol.msmaintenance.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Utility class holding the business rules of the maintenance service. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BusinessConstants {

  /** Days before the due date from which an order is created. */
  public static final int DAYS_AHEAD = 7;

  /** Kilometers before the due odometer from which an order is created. */
  public static final int KM_AHEAD = 500;

  /** Days that the upcoming counters of the statistics look ahead. */
  public static final int UPCOMING_DAYS = 7;

  /** Decimals kept in the costs. */
  public static final int MONEY_SCALE = 2;

  /** Vehicle statuses of ms-vehicles that this service uses. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class VehicleStatus {
    public static final String AVAILABLE = "AVAILABLE";
    public static final String IN_MAINTENANCE = "IN_MAINTENANCE";
  }
}
