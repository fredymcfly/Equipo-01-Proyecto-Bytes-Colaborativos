package com.fleetcontrol.msfuel.client;

import com.fleetcontrol.msfuel.client.dto.VehicleResponse;
import com.fleetcontrol.msfuel.exception.DependencyUnavailableException;
import com.fleetcontrol.msfuel.exception.ErrorCode;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Respuesta cuando el circuit breaker de vehicleClient está abierto o ms-vehicles no contesta. */
@Component
public class VehicleClientFallback implements VehicleClient {

  private static final String SERVICE = "ms-vehicles";

  @Override
  public VehicleResponse getVehicle(UUID vehicleId) {
    throw new DependencyUnavailableException(SERVICE, ErrorCode.SERVICE_UNAVAILABLE);
  }
}
