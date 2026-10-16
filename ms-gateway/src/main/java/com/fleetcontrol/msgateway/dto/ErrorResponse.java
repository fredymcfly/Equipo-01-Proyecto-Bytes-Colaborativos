package com.fleetcontrol.msgateway.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Error body shared by every FleetControl service, as defined in the common contract. {@code
 * service} only appears in {@code SERVICE_UNAVAILABLE} errors and {@code retryAfter} (seconds) only
 * in {@code RATE_LIMIT_EXCEEDED} errors.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
    String error, String message, String service, Long retryAfter, String timestamp) {}
