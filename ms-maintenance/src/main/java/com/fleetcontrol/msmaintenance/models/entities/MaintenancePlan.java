package com.fleetcontrol.msmaintenance.models.entities;

import com.fleetcontrol.msmaintenance.models.enums.MaintenanceType;
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
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A recurring maintenance plan for a vehicle, due by kilometers, by days or by both. */
@Entity
@Table(name = "maintenance_plans")
@Getter
@Setter
@NoArgsConstructor
public class MaintenancePlan {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false)
  private UUID vehicleId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private MaintenanceType type;

  private Integer intervalKm;

  private Integer intervalDays;

  @Column(nullable = false)
  private LocalDate lastDoneAt;

  @Column(nullable = false)
  private Integer lastDoneKm;

  private LocalDate nextDueAt;

  private Integer nextDueKm;

  @Column(nullable = false)
  private Boolean active = true;

  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @PrePersist
  void onCreate() {
    createdAt = Instant.now();
  }

  /** Records an intervention and recalculates when the plan is due next. */
  public void registerDone(LocalDate doneAt, Integer doneKm) {
    lastDoneAt = doneAt;
    lastDoneKm = doneKm;
    nextDueAt = intervalDays == null ? null : doneAt.plusDays(intervalDays);
    nextDueKm = intervalKm == null ? null : doneKm + intervalKm;
  }
}
