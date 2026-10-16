package com.fleetcontrol.msfuel.dto;

import java.time.LocalDate;
import java.util.List;

/** Consumo de la flota en un periodo, vehículo a vehículo. */
public record FuelConsumptionResponse(
    LocalDate from, LocalDate to, List<VehicleConsumption> vehicles) {}
