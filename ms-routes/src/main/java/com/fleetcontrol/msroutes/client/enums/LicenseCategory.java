package com.fleetcontrol.msroutes.client.enums;

import java.util.Set;

public enum LicenseCategory {
  A(Set.of(VehicleType.MOTORCYCLE)),
  B(Set.of(VehicleType.CAR, VehicleType.VAN)),
  C(Set.of(VehicleType.CAR, VehicleType.VAN, VehicleType.TRUCK));

  private final Set<VehicleType> allowedTypes;

  LicenseCategory(Set<VehicleType> allowedTypes) {
    this.allowedTypes = allowedTypes;
  }

  /**
   * Returns whether this license category allows driving the given vehicles type.
   */
  public boolean allows(VehicleType type){
    return allowedTypes.contains(type);
  }
}
