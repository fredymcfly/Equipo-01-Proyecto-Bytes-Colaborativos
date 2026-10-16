package com.fleetcontrol.msdashboard.models.dto.response;

import java.math.BigDecimal;

/**
 * Key indicators of the period. Each value is {@code null} when the service that owns it does not
 * answer: routes for the first two, fuel for the next three and maintenance for the last one.
 */
public record KpisResponse(
    Long routesCompleted,
    BigDecimal distanceKm,
    BigDecimal fuelLiters,
    BigDecimal fuelCost,
    BigDecimal avgConsumptionL100km,
    BigDecimal maintenanceCost) {}
