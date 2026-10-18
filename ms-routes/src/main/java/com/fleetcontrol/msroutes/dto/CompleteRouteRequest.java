package com.fleetcontrol.msroutes.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Request body used to complete a route.
 *
 * @param actualDistanceKm distance really travelled, in kilometres
 * @param notes optional notes about the trip
 */
public record CompleteRouteRequest(
    @Schema(description = "Actual distance in km", example = "362.4") @NotNull @Positive
        BigDecimal actualDistanceKm,
    @Schema(example = "Sin incidencias") @Size(max = 1000) String notes) {}
