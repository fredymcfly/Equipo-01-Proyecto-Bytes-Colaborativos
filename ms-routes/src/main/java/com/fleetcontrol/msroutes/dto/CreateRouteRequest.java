package com.fleetcontrol.msroutes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Request body used to plan a route.
 *
 * @param vehicleId vehicle identifier
 * @param driverId driver identifier
 * @param origin route origin
 * @param destination route destination
 * @param plannedStart planned start timestamp
 * @param estimatedDurationMin estimated duration in minutes
 * @param plannedDistanceKm planned distance in kilometres
 */
public record CreateRouteRequest(
    @NotNull UUID vehicleId,
    @NotNull UUID driverId,
    @NotBlank @Size(max = 255) String origin,
    @NotBlank @Size(max = 255) String destination,
    @NotNull Instant plannedStart,
    @NotNull @Positive Integer estimatedDurationMin,
    @NotNull @Positive BigDecimal plannedDistanceKm) {}
