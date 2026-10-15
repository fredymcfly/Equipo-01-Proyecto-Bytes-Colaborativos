package com.fleetcontrol.msfuel.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Métricas de un periodo de la serie temporal. */
public record FuelStatsPeriod(
    LocalDate period,
    BigDecimal liters,
    BigDecimal cost,
    BigDecimal avgPricePerLiter,
    BigDecimal avgL100km) {}
