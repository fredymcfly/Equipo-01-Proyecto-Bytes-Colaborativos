package com.fleetcontrol.msroutes.seeder;

import com.fleetcontrol.msroutes.client.DriverClient;
import com.fleetcontrol.msroutes.client.VehicleClient;
import com.fleetcontrol.msroutes.client.dto.DriverResponse;
import com.fleetcontrol.msroutes.client.dto.VehicleResponse;
import com.fleetcontrol.msroutes.config.DemoProperties;
import com.fleetcontrol.msroutes.constants.DemoConstants;
import com.fleetcontrol.msroutes.entities.Route;
import com.fleetcontrol.msroutes.repository.RouteRepository;
import feign.FeignException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cloud.client.circuitbreaker.NoFallbackAvailableException;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Generates the demo routes when the service starts with the {@code demo} profile.
 *
 * <p>The routes refer to the demo vehicles and drivers by the ids that ms-vehicles and ms-drivers
 * derive from their number, and their data is read from those services so that every route is
 * valid. Those services may still be starting, so the seeder runs in its own thread and tries again
 * until they answer. The same {@code fleet.demo.seed} always produces the same routes, and nothing
 * is inserted when there are routes already.
 */
@Component
@Profile(DemoConstants.DEMO_PROFILE)
@RequiredArgsConstructor
@Slf4j
public class DemoDataSeeder implements ApplicationRunner {

  private final RouteRepository repository;
  private final VehicleClient vehicleClient;
  private final DriverClient driverClient;
  private final DemoProperties properties;
  private final Clock clock;

  @Override
  public void run(ApplicationArguments args) {
    Thread thread = new Thread(this::seedWhenReady, "demo-route-seeder");
    thread.setDaemon(true);
    thread.start();
  }

  private void seedWhenReady() {
    for (int attempt = 1; attempt <= DemoConstants.RETRY_ATTEMPTS; attempt++) {
      if (trySeed()) {
        return;
      }
      try {
        Thread.sleep(DemoConstants.RETRY_DELAY_MS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return;
      }
    }
    log.warn("Route demo data not seeded: ms-vehicles or ms-drivers never answered. Restart me.");
  }

  /**
   * Seeds the routes if there are none.
   *
   * @return true when the work is done (seeded, or there were routes already); false when the
   *     vehicles or the drivers are not available yet and it must be tried again
   */
  public boolean trySeed() {
    if (repository.count() > 0) {
      log.info("Route demo data already present, nothing to seed");
      return true;
    }
    List<VehicleResponse> vehicles = new ArrayList<>();
    List<DriverResponse> drivers = new ArrayList<>();
    try {
      for (int number = 1; number <= DemoConstants.VEHICLE_COUNT; number++) {
        vehicles.add(vehicleClient.getVehicle(DemoConstants.vehicleId(number)));
      }
      for (int number = 1; number <= DemoConstants.DRIVER_COUNT; number++) {
        drivers.add(driverClient.getDriver(DemoConstants.driverId(number)));
      }
    } catch (FeignException | NoFallbackAvailableException e) {
      log.info("Waiting for the demo vehicles and drivers: {}", e.getMessage());
      return false;
    }
    int seed = properties.seed() == null ? DemoConstants.DEFAULT_SEED : properties.seed();
    int days = properties.days() == null ? DemoConstants.DEFAULT_DAYS : properties.days();
    List<Route> routes =
        DemoRouteGenerator.generate(
            vehicles,
            drivers,
            new Random(seed),
            LocalDate.now(clock.withZone(ZoneOffset.UTC)),
            days,
            DemoConstants.adminId(seed));
    repository.saveAll(routes);
    log.info("Seeded {} demo routes", routes.size());
    return true;
  }
}
