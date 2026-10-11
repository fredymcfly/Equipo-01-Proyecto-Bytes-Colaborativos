package com.fleetcontrol.msvehicles.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A fleet vehicle with its operational status and odometer. */
@Entity
@Table(name = "vehicles")
@Getter
@Setter
@NoArgsConstructor
public class Vehicle {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, unique = true, length = 20)
  private String plate;

  @Column(nullable = false, length = 80)
  private String make;

  @Column(nullable = false, length = 80)
  private String model;

  @Column(nullable = false)
  private Integer year;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private VehicleType type;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private FuelType fuelType;

  @Column(name = "tank_capacity_l", nullable = false)
  private Integer tankCapacityL;

  @Column(nullable = false)
  private Integer odometerKm = 0;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private VehicleStatus status = VehicleStatus.AVAILABLE;

  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant updatedAt;

  @PrePersist
  void onCreate() {
    createdAt = Instant.now();
    updatedAt = createdAt;
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = Instant.now();
  }
}
