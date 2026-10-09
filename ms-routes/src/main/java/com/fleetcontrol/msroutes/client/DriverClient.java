package com.fleetcontrol.msroutes.client;

import com.fleetcontrol.msroutes.client.dto.DriverResponse;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** Client for ms-drivers. */
@FeignClient(name = "ms-dirvers", url = "${drivers.service.url}")
public interface DriverClient {
  @GetMapping("/api/drivers/{driverId}")
  DriverResponse getDriver(@PathVariable UUID driverId);
}
