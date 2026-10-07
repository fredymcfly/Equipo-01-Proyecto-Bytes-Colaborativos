package com.fleetcontrol.msfuel.dto;

import com.fleetcontrol.msfuel.model.FuelType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Repostaje tal y como se devuelve en la API. */
public record FuelRecordResponse(
    UUID id,
    UUID vehicleId,
    Instant refueledAt,
    FuelType fuelType,
    BigDecimal liters,
    BigDecimal pricePerLiter,
    BigDecimal totalCost,
    Integer odometerKm,
    Boolean fullTank,
    String station,
    BigDecimal consumptionL100km) {}
