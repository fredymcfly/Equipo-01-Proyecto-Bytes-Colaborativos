package com.fleetcontrol.msroutes.repository;

import com.fleetcontrol.msroutes.entities.Route;
import com.fleetcontrol.msroutes.enums.RouteStatus;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

/** Builds dynamic filters for route queries. */
public class RouteSpecifications {
  private RouteSpecifications() {}

  /**
   * Combines the optional filters; null values are ignored.
   *
   * @param status route status
   * @param vehicleId assigned vehicle
   * @param driverId assigned driver
   * @param from lower bound (inclusive) for the planned start
   * @param to upper bound (exclusive) for the planned start
   */
  public static Specification<Route> withFilters(
      RouteStatus status, UUID vehicleId, UUID driverId, Instant from, Instant to) {
    return (root, query, criteriaBuilder) -> {
      List<Predicate> predicates = new ArrayList<>();
      if (status != null) {
        predicates.add(criteriaBuilder.equal(root.get("status"), status));
      }
      if (vehicleId != null) {
        predicates.add(criteriaBuilder.equal(root.get("vehicleId"), vehicleId));
      }
      if (driverId != null) {
        predicates.add(criteriaBuilder.equal(root.get("driverId"), driverId));
      }
      if (from != null) {
        predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("plannedStart"), from));
      }
      if (to != null) {
        predicates.add(criteriaBuilder.lessThan(root.get("plannedStart"), to));
      }
      return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
    };
  }
}
