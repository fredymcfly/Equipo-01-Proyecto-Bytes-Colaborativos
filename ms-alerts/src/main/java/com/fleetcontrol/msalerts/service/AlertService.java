package com.fleetcontrol.msalerts.service;

import com.fleetcontrol.msalerts.exception.AlertNotFoundException;
import com.fleetcontrol.msalerts.exception.InvalidAlertStateException;
import com.fleetcontrol.msalerts.models.dto.request.AcknowledgeRequest;
import com.fleetcontrol.msalerts.models.dto.response.AlertResponse;
import com.fleetcontrol.msalerts.models.dto.response.PageResponse;
import com.fleetcontrol.msalerts.models.entities.Alert;
import com.fleetcontrol.msalerts.models.enums.AlertSeverity;
import com.fleetcontrol.msalerts.models.enums.AlertStatus;
import com.fleetcontrol.msalerts.models.enums.AlertType;
import com.fleetcontrol.msalerts.repository.AlertRepository;
import com.fleetcontrol.msalerts.repository.AlertSpecifications;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Business logic for listing and acknowledging alerts. */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AlertService {

  private final AlertRepository repository;
  private final Clock clock;

  /**
   * Returns a page of alerts, optionally filtered, from the most to the least severe and, within
   * the same severity, from the newest.
   */
  public PageResponse<AlertResponse> list(
      AlertStatus status,
      AlertSeverity severity,
      AlertType type,
      UUID vehicleId,
      int page,
      int size) {
    Specification<Alert> filters =
        AlertSpecifications.hasStatus(status)
            .and(AlertSpecifications.hasSeverity(severity))
            .and(AlertSpecifications.hasType(type))
            .and(AlertSpecifications.forVehicle(vehicleId))
            .and(AlertSpecifications.orderedBySeverity());
    return PageResponse.from(
        repository.findAll(filters, PageRequest.of(page, size)).map(AlertResponse::from));
  }

  /**
   * Marks an open alert as acknowledged by the user, keeping the date and the optional comment.
   *
   * @param actor the id of the user that acknowledges, or the name of the caller if it is not an id
   */
  @Transactional
  public AlertResponse acknowledge(UUID alertId, AcknowledgeRequest request, String actor) {
    Alert alert = repository.findById(alertId).orElseThrow(AlertNotFoundException::new);
    if (alert.getStatus() != AlertStatus.OPEN) {
      throw new InvalidAlertStateException();
    }
    alert.setStatus(AlertStatus.ACKNOWLEDGED);
    alert.setAcknowledgedAt(Instant.now(clock));
    alert.setAcknowledgedBy(parseUserId(actor));
    alert.setComment(request == null ? null : request.comment());
    return AlertResponse.from(repository.saveAndFlush(alert));
  }

  private UUID parseUserId(String actor) {
    try {
      return actor == null ? null : UUID.fromString(actor);
    } catch (IllegalArgumentException exception) {
      return null;
    }
  }
}
