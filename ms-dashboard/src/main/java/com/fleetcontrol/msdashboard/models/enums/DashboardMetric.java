package com.fleetcontrol.msdashboard.models.enums;

import com.fleetcontrol.msdashboard.constants.BusinessConstants;
import com.fleetcontrol.msdashboard.constants.ConfigConstants;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Metrics that can be requested as a time series, with their unit and the owner service. */
@Getter
@RequiredArgsConstructor
public enum DashboardMetric {
  DISTANCE_KM(ConfigConstants.Services.ROUTES, BusinessConstants.Units.KILOMETERS),
  FUEL_LITERS(ConfigConstants.Services.FUEL, BusinessConstants.Units.LITERS),
  FUEL_COST(ConfigConstants.Services.FUEL, BusinessConstants.Units.EUROS),
  CONSUMPTION_L100KM(ConfigConstants.Services.FUEL, BusinessConstants.Units.CONSUMPTION),
  MAINTENANCE_COST(ConfigConstants.Services.MAINTENANCE, BusinessConstants.Units.EUROS),
  ALERTS_CREATED(ConfigConstants.Services.ALERTS, BusinessConstants.Units.ALERTS);

  private final String service;
  private final String unit;
}
