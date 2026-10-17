package com.fleetcontrol.msalerts.client;

import com.fleetcontrol.msalerts.constants.ApiPaths;
import com.fleetcontrol.msalerts.constants.ConfigConstants;
import com.fleetcontrol.msalerts.models.dto.internal.VehicleDto;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** Feign client for ms-vehicles. */
@FeignClient(
    name = ConfigConstants.Services.VEHICLES,
    url = ConfigConstants.Urls.VEHICLES,
    configuration = InternalFeignConfig.class)
public interface VehiclesClient {

  /** Data of one vehicle. */
  @GetMapping(ApiPaths.Sources.Vehicles.VEHICLE_ID)
  VehicleDto getVehicle(@PathVariable("vehicleId") UUID vehicleId);
}
