package com.fleetcontrol.msmaintenance.models.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** Payload used to complete a maintenance order. */
public record CompleteOrderRequest(
    @NotNull @PositiveOrZero BigDecimal cost,
    @PositiveOrZero Integer odometerKm,
    @Size(max = 150) String workshop,
    @Size(max = 500) String notes) {}
