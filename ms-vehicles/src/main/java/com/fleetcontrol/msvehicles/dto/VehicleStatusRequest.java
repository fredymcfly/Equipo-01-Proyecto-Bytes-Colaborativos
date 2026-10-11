package com.fleetcontrol.msvehicles.dto;

import com.fleetcontrol.msvehicles.model.VehicleStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/** Payload to change the status of a vehicle and, optionally, its odometer. */
public record VehicleStatusRequest(
    @NotNull VehicleStatus status, @PositiveOrZero Integer odometerKm) {}
