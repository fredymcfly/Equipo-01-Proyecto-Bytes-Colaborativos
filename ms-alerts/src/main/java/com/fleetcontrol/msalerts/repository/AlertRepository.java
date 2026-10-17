package com.fleetcontrol.msalerts.repository;

import com.fleetcontrol.msalerts.models.entities.Alert;
import com.fleetcontrol.msalerts.models.enums.AlertSeverity;
import com.fleetcontrol.msalerts.models.enums.AlertStatus;
import com.fleetcontrol.msalerts.models.enums.AlertType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Data access for alerts. */
public interface AlertRepository
    extends JpaRepository<Alert, UUID>, JpaSpecificationExecutor<Alert> {

  /** Alerts of the given types that are in one of the given statuses. */
  List<Alert> findByTypeInAndStatusIn(
      Collection<AlertType> types, Collection<AlertStatus> statuses);

  /** Creation date and severity of the alerts created in the interval, end excluded. */
  @Query(
      "select a.createdAt as createdAt, a.severity as severity from Alert a "
          + "where a.createdAt >= :start and a.createdAt < :end")
  List<CreatedAlert> findCreatedBetween(@Param("start") Instant start, @Param("end") Instant end);

  /** Number of alerts with the given status for each severity. */
  @Query(
      "select a.severity as severity, count(a) as total from Alert a "
          + "where a.status = :status group by a.severity")
  List<SeverityCount> countBySeverity(@Param("status") AlertStatus status);

  /** Part of an alert needed to build the statistics. */
  interface CreatedAlert {
    Instant getCreatedAt();

    AlertSeverity getSeverity();
  }

  /** Number of alerts of one severity. */
  interface SeverityCount {
    AlertSeverity getSeverity();

    long getTotal();
  }
}
