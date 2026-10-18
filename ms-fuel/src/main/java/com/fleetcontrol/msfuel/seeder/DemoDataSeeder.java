package com.fleetcontrol.msfuel.seeder;

import com.fleetcontrol.msfuel.client.VehicleClient;
import com.fleetcontrol.msfuel.client.dto.VehicleResponse;
import com.fleetcontrol.msfuel.config.DemoProperties;
import com.fleetcontrol.msfuel.exception.ApiException;
import com.fleetcontrol.msfuel.model.FuelRecord;
import com.fleetcontrol.msfuel.repository.FuelRecordRepository;
import feign.FeignException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Genera los repostajes demo cuando el servicio arranca con el perfil {@code demo}.
 *
 * <p>Los repostajes se refieren a los vehículos demo por el id que ms-vehicles deriva de su número,
 * y sus datos (tipo, combustible, depósito y odómetro) se leen de ese servicio para que cada
 * repostaje sea válido. Como ms-vehicles puede estar arrancando, el seeder trabaja en su propio
 * hilo y lo intenta de nuevo hasta que responde. La misma {@code fleet.demo.seed} produce siempre
 * los mismos repostajes y no se inserta nada si ya hay repostajes.
 */
@Component
@Profile(DemoConstants.DEMO_PROFILE)
@RequiredArgsConstructor
@Slf4j
public class DemoDataSeeder implements ApplicationRunner {

  private final FuelRecordRepository repository;
  private final VehicleClient vehicleClient;
  private final DemoProperties properties;
  private final Clock clock;

  @Override
  public void run(ApplicationArguments args) {
    Thread thread = new Thread(this::seedWhenReady, "demo-fuel-seeder");
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
    log.warn("Fuel demo data not seeded: ms-vehicles never answered. Restart this service.");
  }

  /**
   * Inserta los repostajes si no hay ninguno.
   *
   * @return true cuando el trabajo está hecho (se insertaron o ya había); false cuando ms-vehicles
   *     aún no responde y hay que reintentarlo
   */
  public boolean trySeed() {
    if (repository.count() > 0) {
      log.info("Fuel demo data already present, nothing to seed");
      return true;
    }
    List<VehicleResponse> vehicles = new ArrayList<>();
    try {
      for (int number = 1; number <= DemoConstants.VEHICLE_COUNT; number++) {
        vehicles.add(vehicleClient.getVehicle(DemoConstants.vehicleId(number)));
      }
    } catch (FeignException | ApiException e) {
      log.info("Waiting for the demo vehicles: {}", e.getMessage());
      return false;
    }
    int seed = properties.seed() == null ? DemoConstants.DEFAULT_SEED : properties.seed();
    int days = properties.days() == null ? DemoConstants.DEFAULT_DAYS : properties.days();
    List<FuelRecord> records =
        DemoFuelGenerator.generate(vehicles, new Random(seed), clock.instant(), days);
    repository.saveAll(records);
    log.info("Seeded {} demo refuels", records.size());
    return true;
  }
}
