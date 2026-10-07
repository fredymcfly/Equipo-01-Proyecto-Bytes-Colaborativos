package com.fleetcontrol.msfuel.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Cuerpo de la petición que registra un repostaje. */
public record FuelRecordRequest(
    @NotNull UUID vehicleId,
    @NotNull Instant refueledAt,
    @NotNull @Positive BigDecimal liters,
    @NotNull @Positive BigDecimal pricePerLiter,
    @NotNull @PositiveOrZero Integer odometerKm,
    @NotNull Boolean fullTank,
    String station) {}
