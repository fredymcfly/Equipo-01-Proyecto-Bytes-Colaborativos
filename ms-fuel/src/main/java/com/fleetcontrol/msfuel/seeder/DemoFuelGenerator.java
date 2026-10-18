package com.fleetcontrol.msfuel.seeder;

import com.fleetcontrol.msfuel.client.dto.VehicleResponse;
import com.fleetcontrol.msfuel.model.FuelRecord;
import com.fleetcontrol.msfuel.model.FuelType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Genera los repostajes demo a partir de los vehículos reales.
 *
 * <p>Cada vehículo reposta con el depósito lleno {@code REFUELS_PER_VEHICLE} veces a lo largo del
 * historial. Los odómetros son crecientes y el último termina cerca del odómetro actual del
 * vehículo. El consumo de cada tramo sale de los litros y los kilómetros, como lo calcula el
 * servicio, y depende del tipo de vehículo. Dos vehículos gastan un 30 % más en los últimos 30
 * días.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DemoFuelGenerator {

  private static final int SCALE = 2;
  private static final long MINUTES_PER_DAY = 24L * 60;
  private static final double DAYS_PER_WEEK = 7.0;
  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

  /**
   * Genera los repostajes de toda la flota.
   *
   * @param vehicles los vehículos demo, en el orden de su número (el primero es el vehículo 1)
   * @param random generador creado con la semilla de la demo
   * @param now momento actual
   * @param days días de historial
   * @return los repostajes, vehículo a vehículo y del más antiguo al más reciente
   */
  public static List<FuelRecord> generate(
      List<VehicleResponse> vehicles, Random random, Instant now, int days) {
    List<FuelRecord> records = new ArrayList<>();
    for (int index = 0; index < vehicles.size(); index++) {
      boolean highConsumption = DemoConstants.HIGH_CONSUMPTION_VEHICLES.contains(index + 1);
      records.addAll(forVehicle(vehicles.get(index), random, now, days, highConsumption));
    }
    return records;
  }

  private static List<FuelRecord> forVehicle(
      VehicleResponse vehicle, Random random, Instant now, int days, boolean highConsumption) {
    int count = DemoConstants.REFUELS_PER_VEHICLE;
    List<Instant> times = times(random, now, days, count);
    double base = baseConsumption(vehicle);
    Instant highSince = now.minus(Duration.ofDays(DemoConstants.HIGH_CONSUMPTION_DAYS));

    int[] odometers = new int[count];
    BigDecimal[] liters = new BigDecimal[count];
    int current = vehicle.odometerKm() == null ? 0 : vehicle.odometerKm();
    odometers[count - 1] =
        Math.max(0, current - random.nextInt(DemoConstants.MAX_KM_SINCE_LAST_REFUEL + 1));
    for (int index = count - 1; index >= 0; index--) {
      double fill = DemoConstants.MIN_FILL + random.nextDouble() * DemoConstants.FILL_RANGE;
      liters[index] =
          BigDecimal.valueOf(vehicle.tankCapacityL() * fill).setScale(SCALE, RoundingMode.HALF_UP);
      if (index > 0) {
        double factor =
            DemoConstants.CONSUMPTION_MIN_FACTOR
                + random.nextDouble() * DemoConstants.CONSUMPTION_FACTOR_RANGE;
        if (highConsumption && !times.get(index).isBefore(highSince)) {
          factor *= DemoConstants.HIGH_CONSUMPTION_FACTOR;
        }
        long km = Math.max(1, Math.round(liters[index].doubleValue() * 100 / (base * factor)));
        odometers[index - 1] = Math.max(0, odometers[index] - (int) km);
      }
    }

    List<FuelRecord> records = new ArrayList<>();
    for (int index = 0; index < count; index++) {
      Integer previous = index == 0 ? null : odometers[index - 1];
      records.add(
          record(vehicle, times.get(index), liters[index], odometers[index], previous, random));
    }
    return records;
  }

  private static FuelRecord record(
      VehicleResponse vehicle,
      Instant at,
      BigDecimal liters,
      int odometerKm,
      Integer previousOdometerKm,
      Random random) {
    BigDecimal price = price(vehicle, at, random);
    FuelRecord record = new FuelRecord();
    record.setVehicleId(vehicle.id());
    record.setRefueledAt(at);
    record.setFuelType(FuelType.valueOf(vehicle.fuelType()));
    record.setLiters(liters);
    record.setPricePerLiter(price);
    record.setTotalCost(liters.multiply(price).setScale(SCALE, RoundingMode.HALF_UP));
    record.setOdometerKm(odometerKm);
    record.setFullTank(true);
    record.setStation(DemoConstants.STATIONS.get(random.nextInt(DemoConstants.STATIONS.size())));
    if (previousOdometerKm != null && odometerKm > previousOdometerKm) {
      record.setConsumptionL100km(
          liters
              .divide(
                  BigDecimal.valueOf((long) odometerKm - previousOdometerKm),
                  SCALE,
                  RoundingMode.HALF_UP)
              .multiply(HUNDRED)
              .setScale(SCALE, RoundingMode.HALF_UP));
    }
    return record;
  }

  /** Fechas crecientes y repartidas por todo el historial, en horas de trabajo. */
  private static List<Instant> times(Random random, Instant now, int days, int count) {
    double spacing = (double) days * MINUTES_PER_DAY / count;
    Instant start = now.minus(Duration.ofDays(days));
    List<Instant> times = new ArrayList<>();
    for (int index = 0; index < count; index++) {
      double jitter = (random.nextDouble() * 2 - 1) * DemoConstants.SPACING_JITTER;
      long minutes = Math.round((index + 0.5 + jitter) * spacing);
      Instant day = start.plus(Duration.ofMinutes(minutes));
      Instant midnight =
          day.atZone(ZoneOffset.UTC).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant();
      long hour = DemoConstants.FIRST_HOUR + random.nextInt(DemoConstants.HOURS_RANGE);
      Instant refueledAt = midnight.plus(Duration.ofHours(hour));
      times.add(refueledAt.isAfter(now) ? now : refueledAt);
    }
    return times;
  }

  private static double baseConsumption(VehicleResponse vehicle) {
    double base =
        DemoConstants.BASE_CONSUMPTION.getOrDefault(
            vehicle.type(), DemoConstants.DEFAULT_CONSUMPTION);
    return DemoConstants.HYBRID.equals(vehicle.fuelType())
        ? base * DemoConstants.HYBRID_FACTOR
        : base;
  }

  /** Precio entre 1,45 y 1,75 €/l, con una tendencia que cambia cada semana. */
  private static BigDecimal price(VehicleResponse vehicle, Instant at, Random random) {
    double week = Math.floor(at.getEpochSecond() / (DAYS_PER_WEEK * 86_400));
    double value =
        DemoConstants.BASE_PRICE
            + DemoConstants.TREND_AMPLITUDE * Math.sin(week * DemoConstants.TREND_WEEKLY_STEP)
            + (random.nextDouble() * 2 - 1) * DemoConstants.PRICE_NOISE;
    if (DemoConstants.DIESEL.equals(vehicle.fuelType())) {
      value -= DemoConstants.DIESEL_DISCOUNT;
    }
    double clamped = Math.min(DemoConstants.MAX_PRICE, Math.max(DemoConstants.MIN_PRICE, value));
    return BigDecimal.valueOf(clamped).setScale(SCALE, RoundingMode.HALF_UP);
  }
}
