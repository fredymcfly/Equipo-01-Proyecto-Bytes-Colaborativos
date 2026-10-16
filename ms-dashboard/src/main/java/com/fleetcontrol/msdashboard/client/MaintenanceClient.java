package com.fleetcontrol.msdashboard.client;

import com.fleetcontrol.msdashboard.constants.ApiPaths;
import com.fleetcontrol.msdashboard.constants.ConfigConstants;
import com.fleetcontrol.msdashboard.models.dto.internal.MaintenanceOrdersDto;
import com.fleetcontrol.msdashboard.models.dto.internal.MaintenanceStatsDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Feign client for ms-maintenance. Dates are sent as ISO text, so no formatter changes them. */
@FeignClient(
    name = ConfigConstants.Services.MAINTENANCE,
    url = ConfigConstants.Urls.MAINTENANCE,
    configuration = InternalFeignConfig.class)
public interface MaintenanceClient {

  /** Completed orders and their cost over time. */
  @GetMapping(ApiPaths.Sources.Maintenance.STATS)
  MaintenanceStatsDto stats(
      @RequestParam("from") String from,
      @RequestParam("to") String to,
      @RequestParam("granularity") String granularity);

  /** Work orders with the given status, sorted by scheduled date. */
  @GetMapping(ApiPaths.Sources.Maintenance.ORDERS)
  MaintenanceOrdersDto orders(
      @RequestParam("status") String status,
      @RequestParam("page") int page,
      @RequestParam("size") int size);
}
