package com.fleetcontrol.msfuel.dto;

import java.math.BigDecimal;

/** Totales de la serie temporal completa. */
public record StatsTotals(
    BigDecimal liters, BigDecimal cost, BigDecimal avgPricePerLiter, BigDecimal avgL100km) {}
