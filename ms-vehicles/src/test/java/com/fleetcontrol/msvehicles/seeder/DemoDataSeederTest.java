package com.fleetcontrol.msvehicles.seeder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fleetcontrol.msvehicles.config.properties.DemoProperties;
import com.fleetcontrol.msvehicles.models.entities.Vehicle;
import com.fleetcontrol.msvehicles.models.enums.VehicleStatus;
import com.fleetcontrol.msvehicles.models.enums.VehicleType;
import com.fleetcontrol.msvehicles.repository.VehicleRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests of the demo data generation. */
@ExtendWith(MockitoExtension.class)
class DemoDataSeederTest {

  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-10-05T10:30:00Z"), ZoneOffset.UTC);
  private static final int SEED = 42;
  private static final int DAYS = 90;

  @Mock private VehicleRepository repository;
  @Captor private ArgumentCaptor<List<Vehicle>> saved;

  @Test
  void run_generatesTwentyFiveVehiclesWithTheDemoDistribution() {
    List<Vehicle> vehicles = seed(SEED);

    assertThat(vehicles).hasSize(25);
    assertThat(count(vehicles, Vehicle::getType))
        .containsEntry(VehicleType.CAR, 9L)
        .containsEntry(VehicleType.VAN, 10L)
        .containsEntry(VehicleType.TRUCK, 4L)
        .containsEntry(VehicleType.MOTORCYCLE, 2L);
    assertThat(count(vehicles, Vehicle::getStatus))
        .containsEntry(VehicleStatus.AVAILABLE, 14L)
        .containsEntry(VehicleStatus.IN_USE, 8L)
        .containsEntry(VehicleStatus.IN_MAINTENANCE, 2L)
        .containsEntry(VehicleStatus.OUT_OF_SERVICE, 1L);
  }

  @Test
  void run_leavesTheIdsToBeGeneratedWhenTheVehiclesAreSaved() {
    List<Vehicle> vehicles = seed(SEED);

    assertThat(vehicles).extracting(Vehicle::getId).containsOnlyNulls();
  }

  @Test
  void run_withTheSameSeed_producesTheSameData() {
    List<Vehicle> first = seed(SEED);
    List<Vehicle> second = seed(SEED);

    assertThat(second).usingRecursiveComparison().isEqualTo(first);
  }

  @Test
  void run_withAnotherSeed_changesTheData() {
    assertThat(seed(SEED))
        .extracting(Vehicle::getPlate)
        .isNotEqualTo(seed(7).stream().map(Vehicle::getPlate).toList());
  }

  @Test
  void run_generatesValidVehicles() {
    List<Vehicle> vehicles = seed(SEED);

    assertThat(vehicles).extracting(Vehicle::getPlate).doesNotHaveDuplicates();
    assertThat(vehicles)
        .allSatisfy(
            vehicle -> {
              assertThat(vehicle.getPlate()).matches("\\d{4}-[A-Z]{3}");
              assertThat(vehicle.getYear()).isBetween(1990, 2027);
              assertThat(vehicle.getTankCapacityL()).isPositive();
              assertThat(vehicle.getOdometerKm()).isNotNegative();
              assertThat(vehicle.getCreatedAt()).isBefore(CLOCK.instant());
              assertThat(vehicle.getCreatedAt())
                  .isAfterOrEqualTo(CLOCK.instant().minusSeconds(DAYS * 86_400L));
            });
  }

  @Test
  void run_whenThereAreVehicles_insertsNothing() {
    when(repository.count()).thenReturn(1L);

    new DemoDataSeeder(repository, new DemoProperties(SEED, DAYS), CLOCK).run(null);

    verify(repository, never()).saveAll(anyList());
  }

  private List<Vehicle> seed(int seed) {
    when(repository.count()).thenReturn(0L);
    new DemoDataSeeder(repository, new DemoProperties(seed, DAYS), CLOCK).run(null);
    verify(repository, atLeastOnce()).saveAll(saved.capture());
    return saved.getValue();
  }

  private static <K> Map<K, Long> count(List<Vehicle> vehicles, Function<Vehicle, K> key) {
    return vehicles.stream().collect(Collectors.groupingBy(key, Collectors.counting()));
  }
}
