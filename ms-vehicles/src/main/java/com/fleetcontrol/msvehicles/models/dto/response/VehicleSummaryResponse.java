package com.fleetcontrol.msvehicles.models.dto.response;

import com.fleetcontrol.msvehicles.models.enums.FuelType;
import com.fleetcontrol.msvehicles.models.enums.VehicleStatus;
import com.fleetcontrol.msvehicles.models.enums.VehicleType;
import java.util.UUID;

/** List representation of a vehicle, without audit timestamps. */
public record VehicleSummaryResponse(
    UUID id,
    String plate,
    String make,
    String model,
    Integer year,
    VehicleType type,
    FuelType fuelType,
    Integer tankCapacityL,
    Integer odometerKm,
    VehicleStatus status) {}
