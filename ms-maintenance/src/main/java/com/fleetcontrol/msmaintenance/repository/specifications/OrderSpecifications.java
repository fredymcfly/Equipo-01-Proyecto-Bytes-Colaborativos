package com.fleetcontrol.msmaintenance.repository.specifications;

import com.fleetcontrol.msmaintenance.models.entities.MaintenanceOrder;
import com.fleetcontrol.msmaintenance.models.enums.MaintenanceOrderStatus;
import com.fleetcontrol.msmaintenance.models.enums.MaintenanceType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

/** Reusable filters for order queries. */
public final class OrderSpecifications {

  private OrderSpecifications() {}

  /** Matches the orders of the given vehicle, or all of them when it is null. */
  public static Specification<MaintenanceOrder> hasVehicle(UUID vehicleId) {
    return (root, query, builder) ->
        vehicleId == null ? builder.conjunction() : builder.equal(root.get("vehicleId"), vehicleId);
  }

  /** Matches the orders with the given status, or all of them when it is null. */
  public static Specification<MaintenanceOrder> hasStatus(MaintenanceOrderStatus status) {
    return (root, query, builder) ->
        status == null ? builder.conjunction() : builder.equal(root.get("status"), status);
  }

  /** Matches the orders of the given type, or all of them when it is null. */
  public static Specification<MaintenanceOrder> hasType(MaintenanceType type) {
    return (root, query, builder) ->
        type == null ? builder.conjunction() : builder.equal(root.get("type"), type);
  }

  /** Matches the orders scheduled on or before the given date, or all when it is null. */
  public static Specification<MaintenanceOrder> scheduledOnOrBefore(LocalDate date) {
    return (root, query, builder) ->
        date == null
            ? builder.conjunction()
            : builder.lessThanOrEqualTo(root.get("scheduledFor"), date);
  }

  /** Matches the orders scheduled strictly before the given date. */
  public static Specification<MaintenanceOrder> scheduledBefore(LocalDate date) {
    return (root, query, builder) -> builder.lessThan(root.get("scheduledFor"), date);
  }

  /** Matches the orders scheduled between two dates, both included. */
  public static Specification<MaintenanceOrder> scheduledBetween(LocalDate from, LocalDate to) {
    return (root, query, builder) -> builder.between(root.get("scheduledFor"), from, to);
  }

  /** Matches the orders completed from the first instant up to, but not including, the second. */
  public static Specification<MaintenanceOrder> completedBetween(Instant from, Instant until) {
    return (root, query, builder) ->
        builder.and(
            builder.greaterThanOrEqualTo(root.get("completedAt"), from),
            builder.lessThan(root.get("completedAt"), until));
  }
}
