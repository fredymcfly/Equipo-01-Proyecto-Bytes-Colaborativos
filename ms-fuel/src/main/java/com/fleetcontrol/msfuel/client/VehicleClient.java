package com.fleetcontrol.msfuel.client;

import com.fleetcontrol.msfuel.client.dto.VehicleResponse;
import com.fleetcontrol.msfuel.config.InternalFeignConfig;
import com.fleetcontrol.msfuel.config.NoRetryConfig;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** Cliente de ms-vehicles, del que ms-fuel toma el combustible y la capacidad del depósito. */
@FeignClient(
    name = "vehicleClient",
    url = "${vehicles.service.url}",
    configuration = {InternalFeignConfig.class, NoRetryConfig.class},
    fallback = VehicleClientFallback.class)
public interface VehicleClient {

  /**
   * Detalle de un vehículo por su identificador.
   *
   * @param vehicleId identificador del vehículo
   * @return el vehículo, o 404 si no existe
   */
  @GetMapping("/api/vehicles/{vehicleId}")
  VehicleResponse getVehicle(@PathVariable("vehicleId") UUID vehicleId);
}
