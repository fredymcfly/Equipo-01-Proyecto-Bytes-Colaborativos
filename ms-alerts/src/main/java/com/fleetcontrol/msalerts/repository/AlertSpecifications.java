package com.fleetcontrol.msalerts.repository;

import com.fleetcontrol.msalerts.constants.BusinessConstants;
import com.fleetcontrol.msalerts.models.entities.Alert;
import com.fleetcontrol.msalerts.models.enums.AlertSeverity;
import com.fleetcontrol.msalerts.models.enums.AlertStatus;
import com.fleetcontrol.msalerts.models.enums.AlertType;
import jakarta.persistence.criteria.Expression;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

/** Reusable filters and ordering for alert queries. */
public final class AlertSpecifications {

  private static final int CRITICAL_RANK = 3;
  private static final int WARNING_RANK = 2;
  private static final int INFO_RANK = 1;

  private AlertSpecifications() {}

  /** Matches alerts with the given status, or all of them when it is null. */
  public static Specification<Alert> hasStatus(AlertStatus status) {
    return (root, query, builder) ->
        status == null
            ? builder.conjunction()
            : builder.equal(root.get(BusinessConstants.Fields.STATUS), status);
  }

  /** Matches alerts with the given severity, or all of them when it is null. */
  public static Specification<Alert> hasSeverity(AlertSeverity severity) {
    return (root, query, builder) ->
        severity == null
            ? builder.conjunction()
            : builder.equal(root.get(BusinessConstants.Fields.SEVERITY), severity);
  }

  /** Matches alerts of the given type, or all of them when it is null. */
  public static Specification<Alert> hasType(AlertType type) {
    return (root, query, builder) ->
        type == null
            ? builder.conjunction()
            : builder.equal(root.get(BusinessConstants.Fields.TYPE), type);
  }

  /** Matches alerts about the given vehicle, or all of them when it is null. */
  public static Specification<Alert> forVehicle(UUID vehicleId) {
    return (root, query, builder) ->
        vehicleId == null
            ? builder.conjunction()
            : builder.equal(root.get(BusinessConstants.Fields.VEHICLE_ID), vehicleId);
  }

  /**
   * Sorts by severity from the most to the least urgent, then by creation date from the newest, and
   * finally by id so that pages are stable. The severity is stored as text, so it is ranked with a
   * CASE expression. The count query of a page has no use for an order, so it is skipped there.
   */
  public static Specification<Alert> orderedBySeverity() {
    return (root, query, builder) -> {
      if (query != null && !Long.class.equals(query.getResultType())) {
        Expression<Integer> rank =
            builder
                .<Integer>selectCase()
                .when(
                    builder.equal(
                        root.get(BusinessConstants.Fields.SEVERITY), AlertSeverity.CRITICAL),
                    CRITICAL_RANK)
                .when(
                    builder.equal(
                        root.get(BusinessConstants.Fields.SEVERITY), AlertSeverity.WARNING),
                    WARNING_RANK)
                .otherwise(INFO_RANK);
        query.orderBy(
            List.of(
                builder.desc(rank),
                builder.desc(root.get(BusinessConstants.Fields.CREATED_AT)),
                builder.desc(root.get(BusinessConstants.Fields.ID))));
      }
      return builder.conjunction();
    };
  }
}
