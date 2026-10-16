package com.fleetcontrol.msdashboard.client;

import com.fleetcontrol.msdashboard.constants.ApiPaths;
import com.fleetcontrol.msdashboard.constants.ConfigConstants;
import com.fleetcontrol.msdashboard.models.dto.internal.RoutesStatsDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Feign client for ms-routes. Dates are sent as ISO text so that no formatter changes them. */
@FeignClient(
    name = ConfigConstants.Services.ROUTES,
    url = ConfigConstants.Urls.ROUTES,
    configuration = InternalFeignConfig.class)
public interface RoutesClient {

  /** Completed routes and distance over time. */
  @GetMapping(ApiPaths.Sources.Routes.STATS)
  RoutesStatsDto stats(
      @RequestParam("from") String from,
      @RequestParam("to") String to,
      @RequestParam("granularity") String granularity);
}
