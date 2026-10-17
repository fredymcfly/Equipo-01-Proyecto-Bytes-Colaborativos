package com.fleetcontrol.msalerts.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Utility class holding the business rules and limits of the alert service. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BusinessConstants {

  /** Pagination limits shared by every list endpoint. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Pagination {
    public static final String DEFAULT_PAGE = "0";
    public static final String DEFAULT_SIZE = "20";
    public static final int MIN_PAGE = 0;
    public static final int MIN_SIZE = 1;
    public static final int MAX_SIZE = 100;
  }

  /** Maximum length of the text fields of an alert. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class FieldLimits {
    public static final int MESSAGE_MAX = 500;
    public static final int COMMENT_MAX = 500;
    public static final int DEDUP_KEY_MAX = 120;
  }

  /** Names of the entity attributes used to filter and sort. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Fields {
    public static final String ID = "id";
    public static final String TYPE = "type";
    public static final String SEVERITY = "severity";
    public static final String STATUS = "status";
    public static final String VEHICLE_ID = "vehicleId";
    public static final String CREATED_AT = "createdAt";
    public static final String FROM = "from";
    public static final String TO = "to";
    public static final String PARAMS = "params";
  }

  /** Names of the parameters of the rules. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class RuleParams {
    public static final String DAYS_BEFORE = "daysBefore";
    public static final String THRESHOLD_PERCENT = "thresholdPercent";
    public static final int MIN_DAYS_BEFORE = 1;
    public static final int MAX_DAYS_BEFORE = 365;
    public static final int MIN_THRESHOLD_PERCENT = 1;
    public static final int MAX_THRESHOLD_PERCENT = 1000;

    /** Values used when a rule has no value for a parameter. */
    public static final int DEFAULT_MAINTENANCE_DAYS = 7;

    public static final int DEFAULT_LICENSE_DAYS = 30;
    public static final int DEFAULT_THRESHOLD_PERCENT = 25;
  }

  /** Rules of the evaluator. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Evaluation {
    /** Orders and drivers are read from the source services in pages of this size. */
    public static final int PAGE_SIZE = 100;

    public static final int FIRST_PAGE = 0;
    public static final String PENDING_STATUS = "PENDING";
    public static final long RECENT_DAYS = 30;
    public static final long PREVIOUS_DAYS = 60;
    public static final int PERCENT = 100;
  }

  /** Rules of the statistics. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Stats {
    public static final long MAX_RANGE_DAYS = 366;
    public static final String DEFAULT_GRANULARITY = "DAY";
  }

  /** Texts of the alerts. They are shown to the user, so they are written in Spanish. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Messages {
    public static final String MAINTENANCE_DUE =
        "Mantenimiento %s programado para el %s en el vehículo %s";
    public static final String MAINTENANCE_OVERDUE =
        "Mantenimiento %s vencido desde el %s en el vehículo %s";
    public static final String LICENSE_EXPIRING = "La licencia de %s caduca el %s";
    public static final String LICENSE_EXPIRED = "La licencia de %s caducó el %s";
    public static final String HIGH_CONSUMPTION =
        "El consumo del vehículo %s subió un %s%% (%s L/100 km frente a %s L/100 km)";
  }
}
