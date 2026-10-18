package com.fleetcontrol.msmaintenance.constants;

import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Utility class holding the sizes and names used to generate the demo data. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DemoConstants {

  public static final int DEFAULT_SEED = 42;
  public static final int DEFAULT_DAYS = 90;
  public static final int VEHICLE_COUNT = 25;
  public static final int HISTORY_ORDERS = 40;
  public static final int DUE_SOON_ORDERS = 5;
  public static final int OVERDUE_ORDERS = 1;
  public static final int MAX_DAYS_AHEAD = 7;
  public static final int MIN_ODOMETER = 15_000;
  public static final int ODOMETER_RANGE = 70_000;

  /** Days since the last service of a plan: never so many that the plan is already due. */
  public static final int MIN_DAYS_SINCE_SERVICE = 30;

  public static final int MAX_DAYS_SINCE_SERVICE = 300;
  public static final int DUE_MARGIN_DAYS = 8;

  /** Km since the last service: up to this share of the plan interval, so it is not due yet. */
  public static final double KM_WINDOW_RATIO = 0.7;

  public static final int DAYS_ONLY_KM_WINDOW = 20_000;

  public static final int RETRY_ATTEMPTS = 30;
  public static final long RETRY_DELAY_MS = 5_000L;

  public static final List<String> WORKSHOPS =
      List.of(
          "Taller Central Madrid",
          "Neumáticos Norte",
          "Servicio Oficial Getafe",
          "Taller Rápido Alcobendas");
}
