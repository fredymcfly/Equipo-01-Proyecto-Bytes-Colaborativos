package com.fleetcontrol.msgateway.dto;

import java.util.Map;

/**
 * Status of the gateway and of every downstream service.
 *
 * @param gateway status of the gateway itself
 * @param timestamp instant of the check, in ISO-8601 UTC
 * @param services {@code UP} or {@code DOWN} for each service, keyed by service name
 */
public record HealthResponse(String gateway, String timestamp, Map<String, String> services) {}
