package com.fleetcontrol.msvehicles.models.dto.request;

import com.fleetcontrol.msvehicles.models.enums.VehicleStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/** Payload to change the status of a vehicle and, optionally, its odometer. */
public record VehicleStatusRequest(
    @NotNull VehicleStatus status, @PositiveOrZero Integer odometerKm) {}
