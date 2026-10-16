package com.fleetcontrol.msvehicles.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Utility class holding the business rules and limits of the vehicle service. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BusinessConstants {

  /** Oldest manufacturing year accepted for a vehicle. */
  public static final int MIN_YEAR = 1990;

  /** Years after the current one that are still accepted as manufacturing year. */
  public static final int NEXT_YEARS_ALLOWED = 1;

  /** Pagination limits shared by every list endpoint. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Pagination {
    public static final String DEFAULT_PAGE = "0";
    public static final String DEFAULT_SIZE = "20";
    public static final int MIN_PAGE = 0;
    public static final int MIN_SIZE = 1;
    public static final int MAX_SIZE = 100;
  }
}
