package com.fleetcontrol.msvehicles.repository;

import com.fleetcontrol.msvehicles.model.Vehicle;
import com.fleetcontrol.msvehicles.model.VehicleStatus;
import com.fleetcontrol.msvehicles.model.VehicleType;
import org.springframework.data.jpa.domain.Specification;

/** Reusable filters for vehicle queries. */
public final class VehicleSpecifications {

  private VehicleSpecifications() {}

  /** Matches vehicles with the given status, or all of them when it is null. */
  public static Specification<Vehicle> hasStatus(VehicleStatus status) {
    return (root, query, builder) ->
        status == null ? builder.conjunction() : builder.equal(root.get("status"), status);
  }

  /** Matches vehicles with the given type, or all of them when it is null. */
  public static Specification<Vehicle> hasType(VehicleType type) {
    return (root, query, builder) ->
        type == null ? builder.conjunction() : builder.equal(root.get("type"), type);
  }

  /** Matches vehicles whose plate contains the given text, ignoring case. */
  public static Specification<Vehicle> plateContains(String plate) {
    return (root, query, builder) -> {
      if (plate == null || plate.isBlank()) {
        return builder.conjunction();
      }
      return builder.like(builder.lower(root.get("plate")), "%" + plate.toLowerCase() + "%");
    };
  }
}
