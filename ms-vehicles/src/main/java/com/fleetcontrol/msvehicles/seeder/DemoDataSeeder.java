package com.fleetcontrol.msvehicles.seeder;

import com.fleetcontrol.msvehicles.config.properties.DemoProperties;
import com.fleetcontrol.msvehicles.constants.ConfigConstants;
import com.fleetcontrol.msvehicles.constants.DemoConstants;
import com.fleetcontrol.msvehicles.models.entities.Vehicle;
import com.fleetcontrol.msvehicles.models.enums.VehicleStatus;
import com.fleetcontrol.msvehicles.repository.VehicleRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.Year;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Generates the demo vehicles when the service starts with the {@code demo} profile.
 *
 * <p>It creates 25 vehicles of the four types in every status. The same {@code fleet.demo.seed}
 * always produces the same plates, models and statuses, and nothing is inserted when there are
 * vehicles already. The ids are generated when the vehicles are saved.
 */
@Component
@Profile(ConfigConstants.DEMO_PROFILE)
@RequiredArgsConstructor
@Slf4j
public class DemoDataSeeder implements ApplicationRunner {

  private final VehicleRepository repository;
  private final DemoProperties properties;
  private final Clock clock;

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (repository.count() > 0) {
      log.info("Vehicle demo data already present, nothing to seed");
      return;
    }
    Random random =
        new Random(properties.seed() == null ? DemoConstants.DEFAULT_SEED : properties.seed());
    int days = properties.days() == null ? DemoConstants.DEFAULT_DAYS : properties.days();

    List<VehicleStatus> statuses = statusPool(random);
    Set<String> plates = new HashSet<>();
    List<Vehicle> vehicles = new ArrayList<>();
    int number = 1;
    for (DemoConstants.TypeCount group : DemoConstants.TYPE_COUNTS) {
      List<DemoConstants.ModelSpec> models = DemoConstants.CATALOGUE.get(group.type());
      for (int index = 0; index < group.count(); index++) {
        DemoConstants.ModelSpec model = models.get(random.nextInt(models.size()));
        vehicles.add(
            newVehicle(
                group, model, statuses.get(number - 1), uniquePlate(random, plates), days, random));
        number++;
      }
    }
    repository.saveAll(vehicles);
    assignDeterministicIds(vehicles);
    log.info("Seeded {} demo vehicles", vehicles.size());
  }

  private Vehicle newVehicle(
      DemoConstants.TypeCount group,
      DemoConstants.ModelSpec model,
      VehicleStatus status,
      String plate,
      int days,
      Random random) {
    Vehicle vehicle = new Vehicle();
    vehicle.setPlate(plate);
    vehicle.setMake(model.make());
    vehicle.setModel(model.model());
    vehicle.setYear(Year.now(clock).getValue() - random.nextInt(DemoConstants.MAX_AGE_YEARS));
    vehicle.setType(group.type());
    vehicle.setFuelType(model.fuelType());
    vehicle.setTankCapacityL(model.tankCapacityL());
    vehicle.setOdometerKm(
        DemoConstants.MIN_ODOMETER + random.nextInt(DemoConstants.ODOMETER_RANGE));
    vehicle.setStatus(status);
    Instant createdAt =
        clock.instant().minus(Duration.ofDays(1L + random.nextInt(Math.max(days, 1))));
    vehicle.setCreatedAt(createdAt);
    vehicle.setUpdatedAt(createdAt);
    return vehicle;
  }

  /** Statuses in the proportions of the demo, shuffled with the seed so they mix with the types. */
  private List<VehicleStatus> statusPool(Random random) {
    List<VehicleStatus> statuses = new ArrayList<>();
    for (DemoConstants.StatusCount count : DemoConstants.STATUS_COUNTS) {
      statuses.addAll(Collections.nCopies(count.count(), count.status()));
    }
    Collections.shuffle(statuses, random);
    return statuses;
  }

  private String uniquePlate(Random random, Set<String> used) {
    String plate;
    do {
      StringBuilder letters = new StringBuilder();
      for (int index = 0; index < DemoConstants.PLATE_LETTER_COUNT; index++) {
        letters.append(
            DemoConstants.PLATE_LETTERS.charAt(
                random.nextInt(DemoConstants.PLATE_LETTERS.length())));
      }
      plate =
          String.format(
              DemoConstants.PLATE_FORMAT,
              random.nextInt(DemoConstants.PLATE_DIGITS_BOUND),
              letters);
    } while (!used.add(plate));
    return plate;
  }

  /**
   * Gives the vehicle number N the id {@code vehicle-N}, in the order they were generated. The ids
   * are generated when saving, and a generated id cannot be set beforehand, so they are replaced
   * once the vehicles are stored.
   */
  private void assignDeterministicIds(List<Vehicle> vehicles) {
    for (int index = 0; index < vehicles.size(); index++) {
      repository.reassignId(vehicles.get(index).getId(), DemoConstants.vehicleId(index + 1));
    }
  }
}
