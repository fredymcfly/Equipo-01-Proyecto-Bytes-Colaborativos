package com.fleetcontrol.msroutes.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/**
 * Client for ms-vehicles.
 */
@FeignClient(name = "ms-vehicles",url = "${vehicles.service.url}")
public interface VehicleClient {
  @GetMapping("/api/vehicles/{vehicleId}")
  VehicleResponse getVehicle(@PathVariable UUID vehicleId);
}
