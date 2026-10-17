package com.fleetcontrol.msalerts.client;

import com.fleetcontrol.msalerts.constants.ApiPaths;
import com.fleetcontrol.msalerts.constants.ConfigConstants;
import com.fleetcontrol.msalerts.models.dto.internal.DriversPageDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Feign client for ms-drivers. */
@FeignClient(
    name = ConfigConstants.Services.DRIVERS,
    url = ConfigConstants.Urls.DRIVERS,
    configuration = InternalFeignConfig.class)
public interface DriversClient {

  /** Drivers whose license expires within the given days, including the expired licenses. */
  @GetMapping(ApiPaths.Sources.Drivers.LIST)
  DriversPageDto expiringLicenses(
      @RequestParam("licenseExpiringInDays") int days,
      @RequestParam("page") int page,
      @RequestParam("size") int size);
}
