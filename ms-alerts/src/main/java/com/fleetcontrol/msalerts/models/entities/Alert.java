package com.fleetcontrol.msalerts.models.entities;

import com.fleetcontrol.msalerts.constants.BusinessConstants;
import com.fleetcontrol.msalerts.models.enums.AlertSeverity;
import com.fleetcontrol.msalerts.models.enums.AlertStatus;
import com.fleetcontrol.msalerts.models.enums.AlertType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A situation that needs attention, raised by the evaluator. */
@Entity
@Table(name = "alerts")
@Getter
@Setter
@NoArgsConstructor
public class Alert {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private AlertType type;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private AlertSeverity severity;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private AlertStatus status = AlertStatus.OPEN;

  @Column(name = "vehicle_id")
  private UUID vehicleId;

  @Column(name = "driver_id")
  private UUID driverId;

  @Column(nullable = false, length = BusinessConstants.FieldLimits.MESSAGE_MAX)
  private String message;

  @Column(
      name = "dedup_key",
      nullable = false,
      length = BusinessConstants.FieldLimits.DEDUP_KEY_MAX)
  private String dedupKey;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "acknowledged_at")
  private Instant acknowledgedAt;

  @Column(name = "acknowledged_by")
  private UUID acknowledgedBy;

  @Column(length = BusinessConstants.FieldLimits.COMMENT_MAX)
  private String comment;

  @Column(name = "resolved_at")
  private Instant resolvedAt;

  @PrePersist
  void onCreate() {
    if (createdAt == null) {
      createdAt = Instant.now();
    }
  }
}
