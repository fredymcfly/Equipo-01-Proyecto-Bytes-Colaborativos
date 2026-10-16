package com.fleetcontrol.msdashboard.client;

import com.fleetcontrol.msdashboard.constants.ApiPaths;
import com.fleetcontrol.msdashboard.constants.ConfigConstants;
import com.fleetcontrol.msdashboard.models.dto.internal.FleetSummaryDto;
import com.fleetcontrol.msdashboard.models.dto.internal.VehicleDto;
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

  /** Number of vehicles by status and by type. */
  @GetMapping(ApiPaths.Sources.Vehicles.SUMMARY)
  FleetSummaryDto summary();

  /** Data of one vehicle. */
  @GetMapping(ApiPaths.Sources.Vehicles.VEHICLE_ID)
  VehicleDto getVehicle(@PathVariable("vehicleId") UUID vehicleId);
}
