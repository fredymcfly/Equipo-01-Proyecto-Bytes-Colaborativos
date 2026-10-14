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

  public static final List<String> WORKSHOPS =
      List.of(
          "Taller Central Madrid",
          "Neumáticos Norte",
          "Servicio Oficial Getafe",
          "Taller Rápido Alcobendas");
}
