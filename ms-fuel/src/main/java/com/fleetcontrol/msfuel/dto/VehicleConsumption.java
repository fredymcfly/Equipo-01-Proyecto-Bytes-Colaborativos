package com.fleetcontrol.msfuel.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** Consumo agregado de un vehículo dentro de un periodo. */
public record VehicleConsumption(
    UUID vehicleId,
    String plate,
    String type,
    BigDecimal liters,
    BigDecimal cost,
    Integer distanceKm,
    BigDecimal avgL100km) {}
