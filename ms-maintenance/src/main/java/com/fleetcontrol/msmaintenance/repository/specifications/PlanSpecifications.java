package com.fleetcontrol.msmaintenance.repository.specifications;

import com.fleetcontrol.msmaintenance.models.entities.MaintenancePlan;
import com.fleetcontrol.msmaintenance.models.enums.MaintenanceType;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

/** Reusable filters for plan queries. */
public final class PlanSpecifications {

  private PlanSpecifications() {}

  /** Matches the plans of the given vehicle, or all of them when it is null. */
  public static Specification<MaintenancePlan> hasVehicle(UUID vehicleId) {
    return (root, query, builder) ->
        vehicleId == null ? builder.conjunction() : builder.equal(root.get("vehicleId"), vehicleId);
  }

  /** Matches the plans of the given type, or all of them when it is null. */
  public static Specification<MaintenancePlan> hasType(MaintenanceType type) {
    return (root, query, builder) ->
        type == null ? builder.conjunction() : builder.equal(root.get("type"), type);
  }

  /** Matches the plans with the given active flag, or all of them when it is null. */
  public static Specification<MaintenancePlan> hasActive(Boolean active) {
    return (root, query, builder) ->
        active == null ? builder.conjunction() : builder.equal(root.get("active"), active);
  }
}
