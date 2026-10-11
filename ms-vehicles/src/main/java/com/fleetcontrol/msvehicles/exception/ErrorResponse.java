package com.fleetcontrol.msvehicles.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

/** Body returned by every error, as defined by the shared error contract. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
    String error, String message, List<ErrorDetail> details, String service, Instant timestamp) {}
