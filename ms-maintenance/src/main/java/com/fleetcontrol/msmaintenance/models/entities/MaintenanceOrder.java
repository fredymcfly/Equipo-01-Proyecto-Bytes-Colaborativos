package com.fleetcontrol.msmaintenance.models.entities;

import com.fleetcontrol.msmaintenance.models.enums.MaintenanceOrderStatus;
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
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A work order derived from a maintenance plan. */
@Entity
@Table(name = "maintenance_orders")
@Getter
@Setter
@NoArgsConstructor
public class MaintenanceOrder {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false)
  private UUID planId;

  @Column(nullable = false)
  private UUID vehicleId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private MaintenanceType type;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private MaintenanceOrderStatus status = MaintenanceOrderStatus.PENDING;

  @Column(nullable = false)
  private LocalDate scheduledFor;

  private Instant startedAt;

  private Instant completedAt;

  private Integer odometerKm;

  @Column(precision = 10, scale = 2)
  private BigDecimal cost;

  @Column(length = 150)
  private String workshop;

  @Column(length = 500)
  private String notes;

  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @PrePersist
  void onCreate() {
    createdAt = Instant.now();
  }
}
