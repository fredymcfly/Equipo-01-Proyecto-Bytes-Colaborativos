package com.fleetcontrol.msalerts.service.detector;

import com.fleetcontrol.msalerts.client.VehiclesClient;
import com.fleetcontrol.msalerts.models.dto.internal.VehicleDto;
import feign.FeignException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.circuitbreaker.NoFallbackAvailableException;
import org.springframework.stereotype.Component;

/** Finds the plate of a vehicle in ms-vehicles to write readable messages. */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlateResolver {

  private final VehiclesClient vehiclesClient;

  /**
   * Creates a lookup for one evaluation. It asks ms-vehicles once for each vehicle, and if that
   * service does not answer it gives the id, so a missing plate never stops an alert.
   */
  public Function<UUID, String> newLookup() {
    Map<UUID, String> cache = new HashMap<>();
    return vehicleId -> cache.computeIfAbsent(vehicleId, this::fetch);
  }

  private String fetch(UUID vehicleId) {
    try {
      VehicleDto vehicle = vehiclesClient.getVehicle(vehicleId);
      if (vehicle != null && vehicle.plate() != null) {
        return vehicle.plate();
      }
    } catch (FeignException | NoFallbackAvailableException exception) {
      log.warn("Plate of vehicle {} not available: {}", vehicleId, exception.getMessage());
    }
    return vehicleId.toString();
  }
}
