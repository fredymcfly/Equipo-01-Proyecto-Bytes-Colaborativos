package com.fleetcontrol.msroutes.client;

import com.fleetcontrol.msroutes.client.dto.VehicleResponse;
import com.fleetcontrol.msroutes.client.dto.VehicleStatusRequest;
import com.fleetcontrol.msroutes.config.InternalFeignConfig;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Client for ms-vehicles. */
@FeignClient(
    name = "ms-vehicles",
    url = "${VEHICLES_SERVICE_URL}",
    configuration = InternalFeignConfig.class)
public interface VehicleClient {
  @GetMapping("/api/vehicles/{vehicleId}")
  VehicleResponse getVehicle(@PathVariable UUID vehicleId);

  @PatchMapping("/api/vehicles/{vehicleId}/status")
  VehicleResponse updateStatus(
      @PathVariable UUID vehicleId, @RequestBody VehicleStatusRequest request);
}
