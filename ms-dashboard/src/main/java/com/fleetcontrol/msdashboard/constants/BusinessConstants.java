package com.fleetcontrol.msdashboard.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Utility class holding the business rules and limits of the dashboard. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BusinessConstants {

  /** Days of history of the overview and of the ranking. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Days {
    public static final String DEFAULT = "30";
    public static final int MIN = 1;
    public static final int MAX = 365;
  }

  /** Limits of the vehicle ranking. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Ranking {
    public static final String DEFAULT_LIMIT = "5";
    public static final int MIN_LIMIT = 1;
    public static final int MAX_LIMIT = 20;
    public static final int FIRST_RANK = 1;
  }

  /** Rules of the time series. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Timeseries {
    public static final long MAX_RANGE_DAYS = 366;
    public static final String DEFAULT_GRANULARITY = "DAY";
  }

  /** Settings of the calls made by the overview. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Overview {
    /** The totals do not depend on the granularity, so the cheapest one is requested. */
    public static final String KPI_GRANULARITY = "MONTH";

    public static final int UPCOMING_LIMIT = 5;
    public static final int FIRST_PAGE = 0;
    public static final String PENDING_STATUS = "PENDING";
  }

  /** Units of the metrics. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Units {
    public static final String KILOMETERS = "km";
    public static final String LITERS = "L";
    public static final String EUROS = "EUR";
    public static final String CONSUMPTION = "L/100 km";
    public static final String ALERTS = "alerts";
  }

  /** Names of the caches. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Cache {
    public static final String OVERVIEW = "dashboardOverview";
    public static final String TIMESERIES = "dashboardTimeseries";
    public static final String RANKING = "vehiclesRanking";
  }

  /** Names of the request fields used in the validation details. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Fields {
    public static final String METRICS = "metrics";
    public static final String FROM = "from";
    public static final String TO = "to";
  }

  /** Keys of the status counters returned by ms-vehicles. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class VehicleStatuses {
    public static final String AVAILABLE = "AVAILABLE";
    public static final String IN_USE = "IN_USE";
    public static final String IN_MAINTENANCE = "IN_MAINTENANCE";
    public static final String OUT_OF_SERVICE = "OUT_OF_SERVICE";
  }

  /** Keys of the severity counters returned by ms-alerts. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Severities {
    public static final String INFO = "INFO";
    public static final String WARNING = "WARNING";
    public static final String CRITICAL = "CRITICAL";
  }
}
