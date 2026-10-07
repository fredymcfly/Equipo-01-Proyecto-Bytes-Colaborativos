package com.fleetcontrol.msfuel.client.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Vehículo devuelto por ms-vehicles.
 *
 * <p>Los campos enumerados llegan como texto para no acoplar ms-fuel a los enums de ms-vehicles; el
 * servicio convierte lo que necesite.
 */
public record VehicleResponse(
    UUID id,
    String plate,
    String make,
    String model,
    Integer year,
    String type,
    String fuelType,
    Integer tankCapacityL,
    Integer odometerKm,
    String status,
    Instant createdAt,
    Instant updatedAt) {}
