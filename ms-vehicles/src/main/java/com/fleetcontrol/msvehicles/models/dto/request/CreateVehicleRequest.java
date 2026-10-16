package com.fleetcontrol.msvehicles.models.dto.request;

import com.fleetcontrol.msvehicles.models.enums.FuelType;
import com.fleetcontrol.msvehicles.models.enums.VehicleType;
import com.fleetcontrol.msvehicles.validation.ValidYear;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/** Payload used to register a new vehicle. */
public record CreateVehicleRequest(
    @NotBlank String plate,
    @NotBlank String make,
    @NotBlank String model,
    @NotNull @ValidYear Integer year,
    @NotNull VehicleType type,
    @NotNull FuelType fuelType,
    @NotNull @Positive Integer tankCapacityL,
    @NotNull @PositiveOrZero Integer odometerKm) {}
