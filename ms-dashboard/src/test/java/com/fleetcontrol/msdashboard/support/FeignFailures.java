package com.fleetcontrol.msdashboard.support;

import feign.FeignException;
import feign.Request;
import feign.Response;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/** Builds the exception that Feign throws when a source service fails. */
public final class FeignFailures {

  private static final int SERVICE_UNAVAILABLE = 503;

  private FeignFailures() {}

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
