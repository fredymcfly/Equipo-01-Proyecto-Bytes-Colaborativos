package com.fleetcontrol.msvehicles.dto;

import com.fleetcontrol.msvehicles.model.FuelType;
import com.fleetcontrol.msvehicles.model.VehicleStatus;
import com.fleetcontrol.msvehicles.model.VehicleType;
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
