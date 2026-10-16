package com.fleetcontrol.msmaintenance.models.dto.internal;

/** Payload sent to ms-vehicles to change the status and, optionally, the odometer. */
public record VehicleStatusRequest(String status, Integer odometerKm) {}
