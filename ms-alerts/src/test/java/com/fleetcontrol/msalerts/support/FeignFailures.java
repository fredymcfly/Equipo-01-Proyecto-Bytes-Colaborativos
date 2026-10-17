package com.fleetcontrol.msalerts.support;

import feign.FeignException;
import feign.Request;
import feign.Response;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import org.springframework.cloud.client.circuitbreaker.NoFallbackAvailableException;

/** Builds the exception that Feign throws when a source service fails. */
public final class FeignFailures {

  private static final int SERVICE_UNAVAILABLE = 503;

  private FeignFailures() {}

  /** Exception that the circuit breaker throws when a call times out or its circuit is open. */
  public static NoFallbackAvailableException circuitBroken() {
    return new NoFallbackAvailableException("No fallback available.", new TimeoutException());
  }

  /** Exception of a service that answers 503. */
  public static FeignException unavailable() {
    Request request =
        Request.create(
            Request.HttpMethod.GET,
            "http://source/api",
            Map.of(),
            null,
            StandardCharsets.UTF_8,
            null);
    Response response =
        Response.builder()
            .status(SERVICE_UNAVAILABLE)
            .reason("Service Unavailable")
            .request(request)
            .headers(Map.of())
            .build();
    return FeignException.errorStatus("Client#call()", response);
  }
}
