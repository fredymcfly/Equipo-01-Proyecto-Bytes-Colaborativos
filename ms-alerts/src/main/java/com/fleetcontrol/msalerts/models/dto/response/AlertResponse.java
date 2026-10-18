package com.fleetcontrol.msalerts.models.dto.response;

import com.fleetcontrol.msalerts.models.entities.Alert;
import com.fleetcontrol.msalerts.models.enums.AlertSeverity;
import com.fleetcontrol.msalerts.models.enums.AlertStatus;
import com.fleetcontrol.msalerts.models.enums.AlertType;
import java.time.Instant;
import java.util.UUID;

/** Representation of an alert. */
public record AlertResponse(
    UUID id,
    AlertType type,
    AlertSeverity severity,
    AlertStatus status,
    UUID vehicleId,
    UUID driverId,
    String message,
    Instant createdAt,
    Instant acknowledgedAt,
    UUID acknowledgedBy,
    String comment,
    Instant resolvedAt) {

  /** Builds the representation of an alert. */
  public static AlertResponse from(Alert alert) {
    return new AlertResponse(
        alert.getId(),
        alert.getType(),
        alert.getSeverity(),
        alert.getStatus(),
        alert.getVehicleId(),
        alert.getDriverId(),
        alert.getMessage(),
        alert.getCreatedAt(),
        alert.getAcknowledgedAt(),
        alert.getAcknowledgedBy(),
        alert.getComment(),
        alert.getResolvedAt());
  }
}
