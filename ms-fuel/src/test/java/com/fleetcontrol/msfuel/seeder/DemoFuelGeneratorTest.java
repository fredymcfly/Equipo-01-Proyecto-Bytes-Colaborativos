package com.fleetcontrol.msfuel.seeder;

import static org.assertj.core.api.Assertions.assertThat;

import com.fleetcontrol.msfuel.client.dto.VehicleResponse;
import com.fleetcontrol.msfuel.model.FuelRecord;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/** Unit tests of the rules that the generated demo refuels must follow. */
class DemoFuelGeneratorTest {

  private static final Instant NOW = Instant.parse("2026-10-05T10:30:00Z");
  private static final int DAYS = 90;

  private final List<VehicleResponse> vehicles = vehicles();
  private final Map<UUID, VehicleResponse> vehicleById =
      vehicles.stream().collect(Collectors.toMap(VehicleResponse::id, v -> v));

  @Test
  void generate_createsTwelveRefuelsPerVehicle() {
    List<FuelRecord> records = generate(42);

    assertThat(records).hasSize(vehicles.size() * DemoConstants.REFUELS_PER_VEHICLE);
    assertThat(byVehicle(records).values()).allSatisfy(list -> assertThat(list).hasSize(12));
  }

  @Test
  void generate_sameSeed_givesTheSameRecords() {
    assertThat(generate(42))
        .usingRecursiveComparison()
        .ignoringFields("id", "createdAt")
        .isEqualTo(generate(42));
  }

  @Test
  void generate_everyRecord_isValidForItsVehicle() {
    for (FuelRecord record : generate(42)) {
      VehicleResponse vehicle = vehicleById.get(record.getVehicleId());

      assertThat(record.getFuelType().name()).isEqualTo(vehicle.fuelType());
      assertThat(record.getLiters()).isPositive();
      assertThat(record.getLiters())
          .isLessThanOrEqualTo(BigDecimal.valueOf(vehicle.tankCapacityL()));
      assertThat(record.getPricePerLiter())
          .isBetween(new BigDecimal("1.45"), new BigDecimal("1.75"));
      assertThat(record.getTotalCost())
          .isEqualByComparingTo(
              record
                  .getLiters()
                  .multiply(record.getPricePerLiter())
                  .setScale(2, RoundingMode.HALF_UP));
      assertThat(record.getFullTank()).isTrue();
      assertThat(record.getStation()).isNotBlank();
      assertThat(record.getRefueledAt()).isBetween(NOW.minus(Duration.ofDays(DAYS)), NOW);
    }
  }

  @Test
  void generate_odometers_neverGoBackAndEndNearTheCurrentReading() {
    byVehicle(generate(42))
        .forEach(
            (vehicleId, list) -> {
              List<FuelRecord> ordered =
                  list.stream().sorted(Comparator.comparing(FuelRecord::getRefueledAt)).toList();
              for (int index = 1; index < ordered.size(); index++) {
                assertThat(ordered.get(index).getOdometerKm())
                    .isGreaterThan(ordered.get(index - 1).getOdometerKm());
              }
              int current = vehicleById.get(vehicleId).odometerKm();
              int last = ordered.get(ordered.size() - 1).getOdometerKm();
              assertThat(last).isBetween(current - DemoConstants.MAX_KM_SINCE_LAST_REFUEL, current);
            });
  }

  @Test
  void generate_consumption_isCalculatedBetweenFullTanksAndFollowsTheVehicleType() {
    Map<String, Double> averageByType = new java.util.HashMap<>();
    byVehicle(generate(42))
        .forEach(
            (vehicleId, list) -> {
              List<FuelRecord> ordered =
                  list.stream().sorted(Comparator.comparing(FuelRecord::getRefueledAt)).toList();
              assertThat(ordered.get(0).getConsumptionL100km()).isNull();
              for (int index = 1; index < ordered.size(); index++) {
                FuelRecord current = ordered.get(index);
                int km = current.getOdometerKm() - ordered.get(index - 1).getOdometerKm();
                BigDecimal expected =
                    current
                        .getLiters()
                        .divide(BigDecimal.valueOf(km), 2, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                assertThat(current.getConsumptionL100km()).isEqualByComparingTo(expected);
              }
              VehicleResponse vehicle = vehicleById.get(vehicleId);
              if (!DemoConstants.HIGH_CONSUMPTION_VEHICLES.contains(number(vehicle))
                  && !"HYBRID".equals(vehicle.fuelType())) {
                averageByType.merge(
                    vehicle.type(),
                    ordered.stream()
                        .skip(1)
                        .mapToDouble(r -> r.getConsumptionL100km().doubleValue())
                        .average()
                        .orElse(0),
                    (a, b) -> (a + b) / 2);
              }
            });

    averageByType.forEach(
        (type, average) ->
            assertThat(average)
                .isBetween(
                    DemoConstants.BASE_CONSUMPTION.get(type) * 0.9,
                    DemoConstants.BASE_CONSUMPTION.get(type) * 1.1));
  }

  @Test
  void generate_twoVehicles_consumeThirtyPercentMoreInTheLastThirtyDays() {
    Map<UUID, List<FuelRecord>> byVehicle = byVehicle(generate(42));
    Instant since = NOW.minus(Duration.ofDays(30));

    for (int number : DemoConstants.HIGH_CONSUMPTION_VEHICLES) {
      VehicleResponse vehicle = vehicles.get(number - 1);
      List<FuelRecord> list = byVehicle.get(vehicle.id());
      double recent = average(list, r -> !r.getRefueledAt().isBefore(since));
      double before = average(list, r -> r.getRefueledAt().isBefore(since));
      assertThat(recent / before).isBetween(1.15, 1.45);
    }
  }

  private static double average(
      List<FuelRecord> list, java.util.function.Predicate<FuelRecord> filter) {
    return list.stream()
        .filter(filter)
        .filter(r -> r.getConsumptionL100km() != null)
        .mapToDouble(r -> r.getConsumptionL100km().doubleValue())
        .average()
        .orElseThrow();
  }

  private int number(VehicleResponse vehicle) {
    return vehicles.indexOf(vehicle) + 1;
  }

  private List<FuelRecord> generate(int seed) {
    return DemoFuelGenerator.generate(vehicles, new Random(seed), NOW, DAYS);
  }

  private static Map<UUID, List<FuelRecord>> byVehicle(List<FuelRecord> records) {
    return records.stream().collect(Collectors.groupingBy(FuelRecord::getVehicleId));
  }

  private static int tankCapacity(String type) {
    if ("CAR".equals(type)) {
      return 45;
    }
    if ("VAN".equals(type)) {
      return 80;
    }
    if ("TRUCK".equals(type)) {
      return 400;
    }
    return 14;
  }

  private static List<VehicleResponse> vehicles() {
    List<VehicleResponse> list = new ArrayList<>();
    IntStream.rangeClosed(1, DemoConstants.VEHICLE_COUNT)
        .forEach(
            n -> {
              String type = n <= 9 ? "CAR" : n <= 19 ? "VAN" : n <= 23 ? "TRUCK" : "MOTORCYCLE";
              String fuel = "TRUCK".equals(type) || "VAN".equals(type) ? "DIESEL" : "GASOLINE";
              int tank = tankCapacity(type);
              list.add(
                  new VehicleResponse(
                      DemoConstants.vehicleId(n),
                      "PLATE-" + n,
                      "Make",
                      "Model",
                      2020,
                      type,
                      fuel,
                      tank,
                      40_000 + n * 1_000,
                      "AVAILABLE",
                      NOW,
                      NOW));
            });
    return list;
  }
}
