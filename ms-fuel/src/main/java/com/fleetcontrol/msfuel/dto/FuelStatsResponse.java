package com.fleetcontrol.msfuel.dto;

import java.time.LocalDate;
import java.util.List;

/** Serie temporal de litros, coste y consumo de la flota o de un vehículo. */
public record FuelStatsResponse(
    LocalDate from,
    LocalDate to,
    Granularity granularity,
    StatsTotals totals,
    List<FuelStatsPeriod> series) {}
