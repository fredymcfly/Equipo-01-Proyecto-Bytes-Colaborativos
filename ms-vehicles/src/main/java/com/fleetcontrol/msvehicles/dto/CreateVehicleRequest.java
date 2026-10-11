package com.fleetcontrol.msvehicles.dto;

import com.fleetcontrol.msvehicles.model.FuelType;
import com.fleetcontrol.msvehicles.model.VehicleType;
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
