package com.fleetcontrol.msroutes.exeption;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** Error body defined by the common contract. */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiError(
    String error, String message, List<Detail> details, String service, Instant timestamp) {

  /** A field that failed validation. */
  public record Detail(String field, String reason) {}

  /** Build an error with the current time, truncated to seconds. */
  public static ApiError of(String error, String message, List<Detail> details, String service) {
    return new ApiError(
        error, message, details, service, Instant.now().truncatedTo(ChronoUnit.SECONDS));
  }
}
