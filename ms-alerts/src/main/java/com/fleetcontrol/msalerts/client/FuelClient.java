package com.fleetcontrol.msalerts.client;

import com.fleetcontrol.msalerts.constants.ApiPaths;
import com.fleetcontrol.msalerts.constants.ConfigConstants;
import com.fleetcontrol.msalerts.models.dto.internal.FuelConsumptionDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Feign client for ms-fuel. Dates are sent as ISO text so that no formatter changes them. */
@FeignClient(
    name = ConfigConstants.Services.FUEL,
    url = ConfigConstants.Urls.FUEL,
    configuration = InternalFeignConfig.class)
public interface FuelClient {

  /** Liters, cost, distance and consumption of every vehicle. */
  @GetMapping(ApiPaths.Sources.Fuel.CONSUMPTION)
  FuelConsumptionDto consumption(@RequestParam("from") String from, @RequestParam("to") String to);
}
