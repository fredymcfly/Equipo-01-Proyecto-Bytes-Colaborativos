package com.fleetcontrol.msfuel.seeder;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Tamaños, consumos y catálogo con los que se generan los repostajes demo. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DemoConstants {

  public static final String DEMO_PROFILE = "demo";

  public static final int DEFAULT_SEED = 42;
  public static final int DEFAULT_DAYS = 90;

  /** Vehículos que genera ms-vehicles: de {@code vehicleId(1)} a {@code vehicleId(25)}. */
  public static final int VEHICLE_COUNT = 25;

  /** Repostajes por vehículo: 25 vehículos dan unos 300 repostajes. */
  public static final int REFUELS_PER_VEHICLE = 12;

  /** El último repostaje queda como mucho a estos km del odómetro actual del vehículo. */
  public static final int MAX_KM_SINCE_LAST_REFUEL = 120;

  /** Los repostajes de cada vehículo se separan con una variación de hasta este porcentaje. */
  public static final double SPACING_JITTER = 0.2;

  public static final int FIRST_HOUR = 6;
  public static final int HOURS_RANGE = 14;

  /** Los litros de un repostaje son entre el 55 % y el 85 % del depósito. */
  public static final double MIN_FILL = 0.55;

  public static final double FILL_RANGE = 0.30;

  /** El consumo de cada tramo varía entre 0,92 y 1,08 veces el consumo base. */
  public static final double CONSUMPTION_MIN_FACTOR = 0.92;

  public static final double CONSUMPTION_FACTOR_RANGE = 0.16;

  /** Consumo base en l/100 km por tipo de vehículo. */
  public static final Map<String, Double> BASE_CONSUMPTION =
      Map.of("CAR", 6.5, "VAN", 10.5, "TRUCK", 27.0, "MOTORCYCLE", 4.2);

  public static final double DEFAULT_CONSUMPTION = 8.0;

  /** Números de los vehículos que consumen más de lo normal en los últimos 30 días. */
  public static final Set<Integer> HIGH_CONSUMPTION_VEHICLES = Set.of(3, 12);

  public static final double HIGH_CONSUMPTION_FACTOR = 1.30;
  public static final int HIGH_CONSUMPTION_DAYS = 30;

  public static final double BASE_PRICE = 1.60;
  public static final double DIESEL_DISCOUNT = 0.04;
  public static final String DIESEL = "DIESEL";
  public static final double TREND_AMPLITUDE = 0.08;
  public static final double TREND_WEEKLY_STEP = 0.5;
  public static final double PRICE_NOISE = 0.02;
  public static final double MIN_PRICE = 1.45;
  public static final double MAX_PRICE = 1.75;

  public static final int RETRY_ATTEMPTS = 30;
  public static final long RETRY_DELAY_MS = 5_000L;

  public static final List<String> STATIONS =
      List.of(
          "Repsol - A-3 km 12",
          "Cepsa - Polígono Sur",
          "Shell - Ronda Norte",
          "BP - Autovía A-4",
          "Galp - Avenida del Puerto",
          "Moeve - Centro Logístico",
          "Petronor - Carretera Nacional II");

  /**
   * Id del vehículo demo con ese número, desde 1. Es el mismo que asigna ms-vehicles, así que no
   * hace falta preguntárselo.
   */
  public static UUID vehicleId(int number) {
    return UUID.nameUUIDFromBytes(("vehicle-" + number).getBytes(StandardCharsets.UTF_8));
  }
}
