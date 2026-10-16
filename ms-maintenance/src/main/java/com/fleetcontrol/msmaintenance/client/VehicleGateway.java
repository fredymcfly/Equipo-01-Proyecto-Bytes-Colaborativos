package com.fleetcontrol.msmaintenance.client;

import com.fleetcontrol.msmaintenance.constants.ClientConstants;
import com.fleetcontrol.msmaintenance.constants.ConfigConstants;
import com.fleetcontrol.msmaintenance.exception.ApiException;
import com.fleetcontrol.msmaintenance.exception.ServiceUnavailableException;
import com.fleetcontrol.msmaintenance.models.dto.internal.VehicleResponse;
import com.fleetcontrol.msmaintenance.models.dto.internal.VehicleStatusRequest;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.client.circuitbreaker.NoFallbackAvailableException;
import org.springframework.stereotype.Component;

/**
 * Single entry point to ms-vehicles. It retries once the reads, which are safe to repeat, and
 * reports any failure of the service as a {@link ServiceUnavailableException}.
 */
@Component
@RequiredArgsConstructor
public class VehicleGateway {

  private final VehicleClient client;

  /** Reads a vehicle, with its current status and odometer. */
  public VehicleResponse getVehicle(UUID vehicleId) {
    return execute(() -> client.getVehicle(vehicleId), ClientConstants.Attempts.READ);
  }

  /** Changes the status of a vehicle and, when it is not null, its odometer. */
  public VehicleResponse changeStatus(UUID vehicleId, String status, Integer odometerKm) {
    VehicleStatusRequest request = new VehicleStatusRequest(status, odometerKm);
    return execute(() -> client.updateStatus(vehicleId, request), ClientConstants.Attempts.WRITE);
  }

  private <T> T execute(Supplier<T> call, int attempts) {
    ServiceUnavailableException last =
        new ServiceUnavailableException(ConfigConstants.Services.VEHICLES);
    for (int attempt = 0; attempt < attempts; attempt++) {
      try {
        return call.get();
      } catch (ServiceUnavailableException exception) {
        last = exception;
      } catch (ApiException exception) {
        throw exception;
      } catch (NoFallbackAvailableException exception) {
        if (exception.getCause() instanceof ApiException apiException
            && !(apiException instanceof ServiceUnavailableException)) {
          throw apiException;
        }
      } catch (FeignException | CallNotPermittedException exception) {
        last = new ServiceUnavailableException(ConfigConstants.Services.VEHICLES);
      }
    }
    throw last;
  }
}
