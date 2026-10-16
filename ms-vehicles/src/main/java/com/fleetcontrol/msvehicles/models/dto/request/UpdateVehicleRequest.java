package com.fleetcontrol.msvehicles.models.dto.request;

import com.fleetcontrol.msvehicles.models.enums.FuelType;
import com.fleetcontrol.msvehicles.models.enums.VehicleType;
import com.fleetcontrol.msvehicles.validation.ValidYear;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Payload used to update the descriptive data of a vehicle. */
public record UpdateVehicleRequest(
    @NotBlank String make,
    @NotBlank String model,
    @NotNull @ValidYear Integer year,
    @NotNull VehicleType type,
    @NotNull FuelType fuelType,
    @NotNull @Positive Integer tankCapacityL) {}
