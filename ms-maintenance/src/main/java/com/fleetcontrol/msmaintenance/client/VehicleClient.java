package com.fleetcontrol.msmaintenance.client;

import com.fleetcontrol.msmaintenance.constants.ApiPaths;
import com.fleetcontrol.msmaintenance.constants.ConfigConstants;
import com.fleetcontrol.msmaintenance.models.dto.internal.VehicleResponse;
import com.fleetcontrol.msmaintenance.models.dto.internal.VehicleStatusRequest;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Feign client for ms-vehicles. */
@FeignClient(
    name = ConfigConstants.Services.VEHICLES,
    url = "${vehicles.service.url}",
    configuration = VehicleFeignConfig.class)
public interface VehicleClient {

  @GetMapping(ApiPaths.Vehicles.VEHICLE_ID)
  VehicleResponse getVehicle(@PathVariable("vehicleId") UUID vehicleId);

  @PatchMapping(ApiPaths.Vehicles.VEHICLE_ID_STATUS)
  VehicleResponse updateStatus(
      @PathVariable("vehicleId") UUID vehicleId, @RequestBody VehicleStatusRequest request);
}
