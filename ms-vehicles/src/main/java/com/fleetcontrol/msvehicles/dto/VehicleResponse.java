package com.fleetcontrol.msvehicles.dto;

import com.fleetcontrol.msvehicles.model.FuelType;
import com.fleetcontrol.msvehicles.model.VehicleStatus;
import com.fleetcontrol.msvehicles.model.VehicleType;
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
