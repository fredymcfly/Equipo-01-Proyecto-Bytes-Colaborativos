package com.fleetcontrol.msdashboard.models.enums;

import com.fleetcontrol.msdashboard.constants.BusinessConstants;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Values by which the vehicles can be ranked. */
@Getter
@RequiredArgsConstructor
public enum RankingMetric {
  FUEL_COST(BusinessConstants.Units.EUROS),
  CONSUMPTION(BusinessConstants.Units.CONSUMPTION);

  private final String unit;
}
