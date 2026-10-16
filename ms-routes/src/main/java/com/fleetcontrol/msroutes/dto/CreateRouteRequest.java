package com.fleetcontrol.msroutes.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
    @Schema(example = "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a01") @NotNull UUID vehicleId,
    @Schema(example = "3b2d8f10-7c4a-4e55-b1a0-5d9e2c7f1a01") @NotNull UUID driverId,
    @Schema(example = "Madrid - Centro Logístico") @NotBlank @Size(max = 255) String origin,
    @Schema(example = "Valencia - Puerto") @NotBlank @Size(max = 255) String destination,
    @Schema(description = "Planned start in UTC (ISO-8601)", example = "2026-10-06T07:00:00Z")
        @NotNull
        Instant plannedStart,
    @Schema(description = "Estimated duration in minutes", example = "240") @NotNull @Positive
        Integer estimatedDurationMin,
    @Schema(description = "Planned distance in km", example = "355.0") @NotNull @Positive
        BigDecimal plannedDistanceKm) {}
