package com.fleetcontrol.msvehicles.models.dto.response;

import com.fleetcontrol.msvehicles.models.enums.FuelType;
import com.fleetcontrol.msvehicles.models.enums.VehicleStatus;
import com.fleetcontrol.msvehicles.models.enums.VehicleType;
import java.time.Instant;
import java.util.UUID;

/** Detailed representation of a vehicle. */
public record VehicleResponse(
    UUID id,
    String plate,
    String make,
    String model,
    Integer year,
    VehicleType type,
    FuelType fuelType,
    Integer tankCapacityL,
    Integer odometerKm,
    VehicleStatus status,
    Instant createdAt,
    Instant updatedAt) {}
