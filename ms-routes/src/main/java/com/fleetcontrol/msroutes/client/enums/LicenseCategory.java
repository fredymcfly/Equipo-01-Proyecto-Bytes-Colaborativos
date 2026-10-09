package com.fleetcontrol.msroutes.client.enums;

import java.util.Set;

/** Driver license categories and the vehicle types they permit. */
public enum LicenseCategory {
  A(Set.of(VehicleType.MOTORCYCLE)),
  B(Set.of(VehicleType.CAR, VehicleType.VAN)),
  C(Set.of(VehicleType.CAR, VehicleType.VAN, VehicleType.TRUCK));

  private final Set<VehicleType> allowedTypes;

  LicenseCategory(Set<VehicleType> allowedTypes) {
    this.allowedTypes = allowedTypes;
  }

  /**
   * Returns whether this license category allows driving the given vehicle type.
   *
   * @param type vehicle type to check
   * @return true when this category permits the vehicle type
   */
  public boolean allows(VehicleType type) {
    return allowedTypes.contains(type);
  }
}
