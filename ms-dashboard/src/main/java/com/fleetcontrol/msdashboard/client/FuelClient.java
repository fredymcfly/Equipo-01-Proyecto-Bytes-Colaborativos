package com.fleetcontrol.msdashboard.client;

import com.fleetcontrol.msdashboard.constants.ApiPaths;
import com.fleetcontrol.msdashboard.constants.ConfigConstants;
import com.fleetcontrol.msdashboard.models.dto.internal.FuelConsumptionDto;
import com.fleetcontrol.msdashboard.models.dto.internal.FuelStatsDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Feign client for ms-fuel. Dates are sent as ISO text so that no formatter changes them. */
@FeignClient(
    name = ConfigConstants.Services.FUEL,
    url = ConfigConstants.Urls.FUEL,
    configuration = InternalFeignConfig.class)
public interface FuelClient {

  /** Liters, cost and consumption over time. */
  @GetMapping(ApiPaths.Sources.Fuel.STATS)
  FuelStatsDto stats(
      @RequestParam("from") String from,
      @RequestParam("to") String to,
      @RequestParam("granularity") String granularity);

  /** Liters, cost, distance and consumption of every vehicle. */
  @GetMapping(ApiPaths.Sources.Fuel.CONSUMPTION)
  FuelConsumptionDto consumption(@RequestParam("from") String from, @RequestParam("to") String to);
}
