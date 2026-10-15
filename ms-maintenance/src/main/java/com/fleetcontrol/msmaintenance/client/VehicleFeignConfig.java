package com.fleetcontrol.msmaintenance.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetcontrol.msmaintenance.config.properties.InternalProperties;
import com.fleetcontrol.msmaintenance.constants.SecurityConstants;
import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;

/**
 * Settings of the ms-vehicles client. It is not annotated with {@code @Configuration} on purpose,
 * so it only applies to the client that references it.
 */
public class VehicleFeignConfig {

  /** Adds the internal key to every call. */
  @Bean
  public RequestInterceptor internalKeyInterceptor(InternalProperties properties) {
    return template -> template.header(SecurityConstants.INTERNAL_KEY_HEADER, properties.apiKey());
  }

  /** Translates the error answers of ms-vehicles into domain exceptions. */
  @Bean
  public ErrorDecoder vehicleErrorDecoder(ObjectMapper objectMapper) {
    return new VehicleErrorDecoder(objectMapper);
  }
}
