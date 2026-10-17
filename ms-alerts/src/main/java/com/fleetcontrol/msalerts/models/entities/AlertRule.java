package com.fleetcontrol.msalerts.models.entities;

import com.fleetcontrol.msalerts.models.enums.AlertSeverity;
import com.fleetcontrol.msalerts.models.enums.AlertType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Configuration of the evaluation of one type of alert. The type is its identifier. */
@Entity
@Table(name = "alert_rules")
@Getter
@Setter
@NoArgsConstructor
public class AlertRule {

  @Id
  @Enumerated(EnumType.STRING)
  private AlertType type;

  @Column(nullable = false)
  private boolean enabled;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private AlertSeverity severity;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false, columnDefinition = "jsonb")
  private Map<String, Object> params = new HashMap<>();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @PrePersist
  @PreUpdate
  void touch() {
    updatedAt = Instant.now();
  }
}
