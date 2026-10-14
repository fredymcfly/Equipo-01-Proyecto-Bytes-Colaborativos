package com.fleetcontrol.msmaintenance.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetcontrol.msmaintenance.constants.ClientConstants;
import com.fleetcontrol.msmaintenance.constants.ConfigConstants;
import com.fleetcontrol.msmaintenance.exception.InvalidOdometerException;
import com.fleetcontrol.msmaintenance.exception.ServiceUnavailableException;
import com.fleetcontrol.msmaintenance.exception.VehicleNotAvailableException;
import com.fleetcontrol.msmaintenance.exception.VehicleNotFoundException;
import feign.Response;
import feign.codec.ErrorDecoder;
import java.io.IOException;
import java.io.InputStream;

/** Turns the error answers of ms-vehicles into the domain exceptions of this service. */
public class VehicleErrorDecoder implements ErrorDecoder {

  private final ObjectMapper objectMapper;

  /** Creates the decoder with the mapper used to read the error body. */
  public VehicleErrorDecoder(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public Exception decode(String methodKey, Response response) {
    if (response.status() == ClientConstants.Http.NOT_FOUND) {
      return new VehicleNotFoundException();
    }
    if (response.status() == ClientConstants.Http.CONFLICT) {
      String error = readError(response);
      if (ClientConstants.VehicleErrors.INVALID_ODOMETER.equals(error)) {
        return new InvalidOdometerException();
      }
      if (ClientConstants.VehicleErrors.INVALID_STATUS_TRANSITION.equals(error)) {
        return new VehicleNotAvailableException();
      }
    }
    return new ServiceUnavailableException(ConfigConstants.Services.VEHICLES);
  }

  private String readError(Response response) {
    if (response.body() == null) {
      return "";
    }
    try (InputStream stream = response.body().asInputStream()) {
      return objectMapper.readTree(stream).path("error").asText("");
    } catch (IOException exception) {
      return "";
    }
  }
}
