package com.fleetcontrol.msdashboard.client;

import com.fleetcontrol.msdashboard.constants.ApiPaths;
import com.fleetcontrol.msdashboard.constants.ConfigConstants;
import com.fleetcontrol.msdashboard.models.dto.internal.AlertsStatsDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Feign client for ms-alerts. Dates are sent as ISO text so that no formatter changes them. */
@FeignClient(
    name = ConfigConstants.Services.ALERTS,
    url = ConfigConstants.Urls.ALERTS,
    configuration = InternalFeignConfig.class)
public interface AlertsClient {

  /** Alerts created over time and open alerts by severity. */
  @GetMapping(ApiPaths.Sources.Alerts.STATS)
  AlertsStatsDto stats(
      @RequestParam("from") String from,
      @RequestParam("to") String to,
      @RequestParam("granularity") String granularity);
}
