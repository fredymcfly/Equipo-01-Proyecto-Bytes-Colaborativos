package com.fleetcontrol.msalerts.client;

import com.fleetcontrol.msalerts.constants.ApiPaths;
import com.fleetcontrol.msalerts.constants.ConfigConstants;
import com.fleetcontrol.msalerts.models.dto.internal.MaintenanceOrdersDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Feign client for ms-maintenance. Dates are sent as ISO text, so no formatter changes them. */
@FeignClient(
    name = ConfigConstants.Services.MAINTENANCE,
    url = ConfigConstants.Urls.MAINTENANCE,
    configuration = InternalFeignConfig.class)
public interface MaintenanceClient {

  /** Orders with the given status scheduled on the given day or before it. */
  @GetMapping(ApiPaths.Sources.Maintenance.ORDERS)
  MaintenanceOrdersDto orders(
      @RequestParam("status") String status,
      @RequestParam("dueBefore") String dueBefore,
      @RequestParam("page") int page,
      @RequestParam("size") int size);
}
