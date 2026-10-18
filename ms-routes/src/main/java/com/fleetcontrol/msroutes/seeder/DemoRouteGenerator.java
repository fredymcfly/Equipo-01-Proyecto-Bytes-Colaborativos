package com.fleetcontrol.msroutes.seeder;

import com.fleetcontrol.msroutes.client.dto.DriverResponse;
import com.fleetcontrol.msroutes.client.dto.VehicleResponse;
import com.fleetcontrol.msroutes.client.enums.DriverStatus;
import com.fleetcontrol.msroutes.client.enums.VehicleStatus;
import com.fleetcontrol.msroutes.constants.DemoConstants;
import com.fleetcontrol.msroutes.entities.Route;
import com.fleetcontrol.msroutes.enums.RouteStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Generates the demo routes from the vehicles and drivers that really exist.
 *
 * <p>The history has two shifts per working day, and in each one a vehicle and a driver take at
 * most one route, so a vehicle or a driver never has two overlapping routes. The vehicles are not
 * out of service, the drivers are active, their license is valid on the day and their category
 * allows the vehicle type. The odometers of every vehicle are consecutive and end at the current
 * odometer of the vehicle. A few routes are planned for the next working days.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DemoRouteGenerator {

  private static final int[] SHIFT_FIRST_MINUTES = {
    DemoConstants.MORNING_FIRST_MINUTE, DemoConstants.AFTERNOON_FIRST_MINUTE
  };
  private static final int[] SHIFT_RANGES = {
    DemoConstants.MORNING_RANGE_MIN, DemoConstants.AFTERNOON_RANGE_MIN
  };

  /**
   * Generates the completed routes of the last days and the planned routes of the next ones.
   *
   * @param vehicles the demo vehicles, in a stable order
   * @param drivers the demo drivers, in a stable order
   * @param random random generator created with the seed of the demo
   * @param today current day (UTC)
   * @param days how many days of history are generated
   * @param createdBy id of the user that creates the routes
   * @return the routes, the completed ones in chronological order and then the planned ones
   */
  public static List<Route> generate(
      List<VehicleResponse> vehicles,
      List<DriverResponse> drivers,
      Random random,
      LocalDate today,
      int days,
      UUID createdBy) {
    List<VehicleResponse> usable =
        vehicles.stream().filter(v -> v.status() != VehicleStatus.OUT_OF_SERVICE).toList();
    List<Route> routes = new ArrayList<>(history(usable, drivers, random, today, days, createdBy));
    assignOdometers(routes, vehicles);
    routes.addAll(planned(vehicles, drivers, random, today, createdBy));
    return routes;
  }

  private static List<Route> history(
      List<VehicleResponse> vehicles,
      List<DriverResponse> drivers,
      Random random,
      LocalDate today,
      int days,
      UUID createdBy) {
    List<Route> routes = new ArrayList<>();
    for (LocalDate day = today.minusDays(days); day.isBefore(today); day = day.plusDays(1)) {
      if (isWeekend(day)) {
        continue;
      }
      for (int shift = 0; shift < SHIFT_FIRST_MINUTES.length; shift++) {
        routes.addAll(shift(vehicles, drivers, random, day, shift, createdBy));
      }
    }
    return routes;
  }

  private static List<Route> shift(
      List<VehicleResponse> vehicles,
      List<DriverResponse> drivers,
      Random random,
      LocalDate day,
      int shift,
      UUID createdBy) {
    List<VehicleResponse> shuffled = new ArrayList<>(vehicles);
    Collections.shuffle(shuffled, random);
    int target =
        DemoConstants.MIN_ROUTES_PER_SHIFT + random.nextInt(DemoConstants.ROUTES_PER_SHIFT_RANGE);
    Set<UUID> busyDrivers = new HashSet<>();
    List<Route> routes = new ArrayList<>();
    for (VehicleResponse vehicle : shuffled) {
      if (routes.size() >= target) {
        break;
      }
      DriverResponse driver = pickDriver(drivers, vehicle, day, busyDrivers, random);
      if (driver == null) {
        continue;
      }
      busyDrivers.add(driver.id());
      int startMinute = SHIFT_FIRST_MINUTES[shift] + random.nextInt(SHIFT_RANGES[shift]);
      routes.add(completed(vehicle, driver, day, startMinute, random, createdBy));
    }
    return routes;
  }

  private static Route completed(
      VehicleResponse vehicle,
      DriverResponse driver,
      LocalDate day,
      int startMinute,
      Random random,
      UUID createdBy) {
    Route route = plan(vehicle, driver, day, startMinute, random, createdBy);
    BigDecimal actualKm =
        scaled(
            route.getPlannedDistanceKm(),
            DemoConstants.ACTUAL_DISTANCE_MIN_FACTOR
                + random.nextDouble() * DemoConstants.ACTUAL_DISTANCE_FACTOR_RANGE);
    long delay = random.nextInt(DemoConstants.MAX_START_DELAY_MIN + 1);
    long duration =
        Math.max(
            1,
            Math.round(
                route.getEstimatedDurationMin()
                    * (DemoConstants.ACTUAL_DURATION_MIN_FACTOR
                        + random.nextDouble() * DemoConstants.ACTUAL_DURATION_FACTOR_RANGE)));
    Instant startedAt = route.getPlannedStart().plusSeconds(delay * 60);
    route.setStatus(RouteStatus.COMPLETED);
    route.setStartedAt(startedAt);
    route.setEndedAt(startedAt.plusSeconds(duration * 60));
    route.setActualDistanceKm(actualKm);
    if (random.nextInt(DemoConstants.NOTE_FREQUENCY) == 0) {
      route.setNotes(DemoConstants.NOTES.get(random.nextInt(DemoConstants.NOTES.size())));
    }
    return route;
  }

  /** A route in PLANNED status; the completed routes are built from it. */
  private static Route plan(
      VehicleResponse vehicle,
      DriverResponse driver,
      LocalDate day,
      int startMinute,
      Random random,
      UUID createdBy) {
    DemoConstants.Trip trip = DemoConstants.TRIPS.get(random.nextInt(DemoConstants.TRIPS.size()));
    BigDecimal plannedKm =
        scaled(
            BigDecimal.valueOf(trip.distanceKm()),
            DemoConstants.PLANNED_DISTANCE_MIN_FACTOR
                + random.nextDouble() * DemoConstants.PLANNED_DISTANCE_FACTOR_RANGE);
    int speed =
        DemoConstants.MIN_AVERAGE_SPEED_KMH + random.nextInt(DemoConstants.AVERAGE_SPEED_RANGE_KMH);
    int duration =
        (int)
            Math.min(
                DemoConstants.MAX_DURATION_MIN,
                Math.max(
                    DemoConstants.MIN_DURATION_MIN,
                    Math.round(plannedKm.doubleValue() / speed * 60)));
    return Route.builder()
        .vehicleId(vehicle.id())
        .driverId(driver.id())
        .origin(trip.origin())
        .destination(trip.destination())
        .plannedStart(day.atStartOfDay(ZoneOffset.UTC).plusMinutes(startMinute).toInstant())
        .estimatedDurationMin(duration)
        .plannedDistanceKm(plannedKm)
        .status(RouteStatus.PLANNED)
        .createdBy(createdBy)
        .build();
  }

  /** Planned routes for different vehicles that are available now, on the next working days. */
  private static List<Route> planned(
      List<VehicleResponse> vehicles,
      List<DriverResponse> drivers,
      Random random,
      LocalDate today,
      UUID createdBy) {
    List<VehicleResponse> available =
        new ArrayList<>(
            vehicles.stream().filter(v -> v.status() == VehicleStatus.AVAILABLE).toList());
    Collections.shuffle(available, random);
    Set<UUID> usedDrivers = new HashSet<>();
    List<Route> routes = new ArrayList<>();
    LocalDate day = today;
    for (VehicleResponse vehicle : available) {
      if (routes.size() >= DemoConstants.PLANNED_ROUTES) {
        break;
      }
      day = nextWorkingDay(day);
      DriverResponse driver = pickDriver(drivers, vehicle, day, usedDrivers, random);
      if (driver != null) {
        usedDrivers.add(driver.id());
        routes.add(
            plan(vehicle, driver, day, DemoConstants.PLANNED_START_MINUTE, random, createdBy));
      }
    }
    return routes;
  }

  /** Gives every vehicle consecutive odometers that end at its current reading. */
  private static void assignOdometers(List<Route> routes, List<VehicleResponse> vehicles) {
    Map<UUID, Integer> total = new HashMap<>();
    for (Route route : routes) {
      total.merge(route.getVehicleId(), kilometres(route), Integer::sum);
    }
    Map<UUID, Integer> next = new HashMap<>();
    for (VehicleResponse vehicle : vehicles) {
      int current = vehicle.odometerKm() == null ? 0 : vehicle.odometerKm();
      next.put(vehicle.id(), Math.max(0, current - total.getOrDefault(vehicle.id(), 0)));
    }
    for (Route route : routes) {
      int start = next.get(route.getVehicleId());
      int end = start + kilometres(route);
      route.setStartOdometerKm(start);
      route.setEndOdometerKm(end);
      next.put(route.getVehicleId(), end);
    }
  }

  private static int kilometres(Route route) {
    return route.getActualDistanceKm().setScale(0, RoundingMode.HALF_UP).intValueExact();
  }

  private static DriverResponse pickDriver(
      List<DriverResponse> drivers,
      VehicleResponse vehicle,
      LocalDate day,
      Set<UUID> busy,
      Random random) {
    List<DriverResponse> candidates =
        drivers.stream()
            .filter(d -> d.status() == DriverStatus.ACTIVE)
            .filter(d -> !d.licenseExpiresAt().isBefore(day))
            .filter(d -> d.licenseCategory().allows(vehicle.type()))
            .filter(d -> !busy.contains(d.id()))
            .toList();
    return candidates.isEmpty() ? null : candidates.get(random.nextInt(candidates.size()));
  }

  private static BigDecimal scaled(BigDecimal value, double factor) {
    return value.multiply(BigDecimal.valueOf(factor)).setScale(1, RoundingMode.HALF_UP);
  }

  private static LocalDate nextWorkingDay(LocalDate day) {
    LocalDate next = day.plusDays(1);
    while (isWeekend(next)) {
      next = next.plusDays(1);
    }
    return next;
  }

  private static boolean isWeekend(LocalDate day) {
    return day.getDayOfWeek() == DayOfWeek.SATURDAY || day.getDayOfWeek() == DayOfWeek.SUNDAY;
  }
}
