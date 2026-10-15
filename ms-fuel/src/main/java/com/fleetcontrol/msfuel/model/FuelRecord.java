package com.fleetcontrol.msfuel.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

/** Repostaje registrado para un vehículo de la flota. */
@Getter
@Setter
@Entity
@Table(name = "fuel_records")
public class FuelRecord {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "vehicle_id", nullable = false, updatable = false)
  private UUID vehicleId;

  @Column(name = "refueled_at", nullable = false)
  private Instant refueledAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "fuel_type", nullable = false, length = 20)
  private FuelType fuelType;

  @Column(name = "liters", nullable = false, precision = 10, scale = 2)
  private BigDecimal liters;

  @Column(name = "price_per_liter", nullable = false, precision = 8, scale = 2)
  private BigDecimal pricePerLiter;

  @Column(name = "total_cost", nullable = false, precision = 12, scale = 2)
  private BigDecimal totalCost;

  @Column(name = "odometer_km", nullable = false)
  private Integer odometerKm;

  @Column(name = "full_tank", nullable = false)
  private Boolean fullTank;

  @Column(name = "station", length = 150)
  private String station;

  @Column(name = "consumption_l100km", precision = 6, scale = 2)
  private BigDecimal consumptionL100km;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;
}
