package com.fleetcontrol.msroutes.seeder;

import static org.assertj.core.api.Assertions.assertThat;

import com.fleetcontrol.msroutes.client.dto.DriverResponse;
import com.fleetcontrol.msroutes.client.dto.VehicleResponse;
import com.fleetcontrol.msroutes.client.enums.DriverStatus;
import com.fleetcontrol.msroutes.client.enums.LicenseCategory;
import com.fleetcontrol.msroutes.client.enums.VehicleStatus;
import com.fleetcontrol.msroutes.client.enums.VehicleType;
import com.fleetcontrol.msroutes.constants.DemoConstants;
import com.fleetcontrol.msroutes.entities.Route;
import com.fleetcontrol.msroutes.enums.RouteStatus;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/** Unit tests of the rules that the generated demo routes must follow. */
class DemoRouteGeneratorTest {

  private static final LocalDate TODAY = LocalDate.of(2026, 10, 5);
  private static final Instant NOW = TODAY.atTime(12, 0).toInstant(ZoneOffset.UTC);
  private static final int DAYS = 90;
  private static final UUID ADMIN = DemoConstants.adminId(42);

  private final List<VehicleResponse> vehicles = vehicles();
  private final List<DriverResponse> drivers = drivers();
  private final Map<UUID, VehicleResponse> vehicleById =
      vehicles.stream().collect(Collectors.toMap(VehicleResponse::id, Function.identity()));
  private final Map<UUID, DriverResponse> driverById =
      drivers.stream().collect(Collectors.toMap(DriverResponse::id, Function.identity()));

  @Test
  void generate_createsAboutThirteenHundredCompletedRoutesAndThreePlanned() {
    List<Route> routes = generate(42);

    assertThat(completed(routes).size()).isBetween(1_100, 1_500);
    assertThat(planned(routes)).hasSize(DemoConstants.PLANNED_ROUTES);
  }

  @Test
  void generate_sameSeed_givesTheSameRoutes() {
    List<Route> first = generate(42);
    List<Route> second = generate(42);

    assertThat(second)
        .usingRecursiveComparison()
        .ignoringFields("id", "createdAt", "updatedAt")
        .isEqualTo(first);
    assertThat(generate(7))
        .extracting(Route::getDriverId)
        .isNotEqualTo(first.stream().map(Route::getDriverId).toList());
  }

  @Test
  void generate_completedRoutes_areOnWorkingDaysInsideTheHistory() {
    for (Route route : completed(generate(42))) {
      LocalDate day = route.getPlannedStart().atZone(ZoneOffset.UTC).toLocalDate();
      assertThat(day).isBetween(TODAY.minusDays(DAYS), TODAY.minusDays(1));
      assertThat(day.getDayOfWeek()).isNotIn(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
      assertThat(route.getEndedAt()).isAfter(route.getStartedAt());
      assertThat(route.getEndedAt()).isBefore(TODAY.atStartOfDay(ZoneOffset.UTC).toInstant());
    }
  }

  @Test
  void generate_everyRoute_hasAnEligibleVehicleAndDriver() {
    for (Route route : generate(42)) {
      VehicleResponse vehicle = vehicleById.get(route.getVehicleId());
      DriverResponse driver = driverById.get(route.getDriverId());
      LocalDate day = route.getPlannedStart().atZone(ZoneOffset.UTC).toLocalDate();

      assertThat(vehicle.status()).isNotEqualTo(VehicleStatus.OUT_OF_SERVICE);
      assertThat(driver.status()).isEqualTo(DriverStatus.ACTIVE);
      assertThat(driver.licenseExpiresAt()).isAfterOrEqualTo(day);
      assertThat(driver.licenseCategory().allows(vehicle.type())).isTrue();
      assertThat(route.getCreatedBy()).isEqualTo(ADMIN);
    }
  }

  @Test
  void generate_neverOverlapsRoutesOfVehicleOrDriver() {
    List<Route> routes = generate(42);

    assertNoOverlap(routes, Route::getVehicleId);
    assertNoOverlap(routes, Route::getDriverId);
  }

  @Test
  void generate_odometers_areConsecutiveAndEndAtTheCurrentReading() {
    List<Route> routes = completed(generate(42));

    Map<UUID, List<Route>> byVehicle =
        routes.stream().collect(Collectors.groupingBy(Route::getVehicleId));
    byVehicle.forEach(
        (vehicleId, list) -> {
          List<Route> ordered =
              list.stream().sorted(Comparator.comparing(Route::getStartedAt)).toList();
          for (int index = 1; index < ordered.size(); index++) {
            assertThat(ordered.get(index).getStartOdometerKm())
                .isEqualTo(ordered.get(index - 1).getEndOdometerKm());
          }
          Route last = ordered.get(ordered.size() - 1);
          assertThat(last.getEndOdometerKm()).isEqualTo(vehicleById.get(vehicleId).odometerKm());
        });
    routes.forEach(
        route ->
            assertThat(route.getEndOdometerKm() - route.getStartOdometerKm())
                .isEqualTo(
                    route.getActualDistanceKm().setScale(0, RoundingMode.HALF_UP).intValue()));
  }

  @Test
  void generate_plannedRoutes_useAvailableVehiclesOnFutureWorkingDays() {
    List<Route> planned = planned(generate(42));

    assertThat(planned).extracting(Route::getVehicleId).doesNotHaveDuplicates();
    for (Route route : planned) {
      LocalDate day = route.getPlannedStart().atZone(ZoneOffset.UTC).toLocalDate();
      assertThat(day).isAfter(TODAY);
      assertThat(day.getDayOfWeek()).isNotIn(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
      assertThat(vehicleById.get(route.getVehicleId()).status()).isEqualTo(VehicleStatus.AVAILABLE);
      assertThat(route.getStartedAt()).isNull();
      assertThat(route.getStartOdometerKm()).isNull();
    }
  }

  @Test
  void generate_noEligibleDriver_createsNoRoutes() {
    List<DriverResponse> suspended =
        drivers.stream()
            .map(
                d ->
                    new DriverResponse(
                        d.id(), d.licenseCategory(), d.licenseExpiresAt(), DriverStatus.SUSPENDED))
            .toList();

    List<Route> routes =
        DemoRouteGenerator.generate(vehicles, suspended, new Random(42), NOW, DAYS, ADMIN);

    assertThat(routes).isEmpty();
  }

  private List<Route> generate(int seed) {
    return DemoRouteGenerator.generate(vehicles, drivers, new Random(seed), NOW, DAYS, ADMIN);
  }

  private static List<Route> completed(List<Route> routes) {
    return routes.stream().filter(r -> r.getStatus() == RouteStatus.COMPLETED).toList();
  }

  private static List<Route> planned(List<Route> routes) {
    return routes.stream().filter(r -> r.getStatus() == RouteStatus.PLANNED).toList();
  }

  private static void assertNoOverlap(List<Route> routes, Function<Route, UUID> owner) {
    Map<UUID, List<Route>> byOwner = routes.stream().collect(Collectors.groupingBy(owner));
    byOwner.values().forEach(DemoRouteGeneratorTest::assertNoOverlap);
  }

  private static void assertNoOverlap(List<Route> routes) {
    List<Route> ordered =
        routes.stream().sorted(Comparator.comparing(Route::getPlannedStart)).toList();
    for (int index = 1; index < ordered.size(); index++) {
      Route previous = ordered.get(index - 1);
      Instant previousEnd = latestEnd(previous);
      assertThat(ordered.get(index).getPlannedStart()).isAfterOrEqualTo(previousEnd);
    }
  }

  /** Planned routes end at the estimated time; completed ones, when they really ended. */
  private static Instant latestEnd(Route route) {
    Instant planned =
        route.getPlannedStart().plus(Duration.ofMinutes(route.getEstimatedDurationMin()));
    return route.getEndedAt() != null && route.getEndedAt().isAfter(planned)
        ? route.getEndedAt()
        : planned;
  }

  private static List<VehicleResponse> vehicles() {
    List<VehicleResponse> list = new ArrayList<>();
    IntStream.rangeClosed(1, DemoConstants.VEHICLE_COUNT)
        .forEach(
            n -> {
              VehicleType type =
                  n <= 3
                      ? VehicleType.TRUCK
                      : n <= 6
                          ? VehicleType.MOTORCYCLE
                          : n % 2 == 0 ? VehicleType.VAN : VehicleType.CAR;
              VehicleStatus status =
                  n == 7
                      ? VehicleStatus.OUT_OF_SERVICE
                      : n == 8
                          ? VehicleStatus.IN_MAINTENANCE
                          : n % 5 == 0 ? VehicleStatus.IN_USE : VehicleStatus.AVAILABLE;
              list.add(
                  new VehicleResponse(
                      DemoConstants.vehicleId(n), type, status, 30_000 + n * 1_000));
            });
    return list;
  }

  private static List<DriverResponse> drivers() {
    List<DriverResponse> list = new ArrayList<>();
    IntStream.rangeClosed(1, DemoConstants.DRIVER_COUNT)
        .forEach(
            n -> {
              LicenseCategory category =
                  n <= 3 ? LicenseCategory.A : n <= 14 ? LicenseCategory.B : LicenseCategory.C;
              DriverStatus status =
                  n == 4
                      ? DriverStatus.SUSPENDED
                      : n == 5 ? DriverStatus.ON_LEAVE : DriverStatus.ACTIVE;
              LocalDate expires = n == 6 ? TODAY.minusDays(18) : TODAY.plusDays(400);
              list.add(new DriverResponse(DemoConstants.driverId(n), category, expires, status));
            });
    return list;
  }
}
