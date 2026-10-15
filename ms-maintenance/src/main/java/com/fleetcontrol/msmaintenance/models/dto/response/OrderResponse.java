package com.fleetcontrol.msmaintenance.models.dto.response;

import com.fleetcontrol.msmaintenance.models.enums.MaintenanceOrderStatus;
import com.fleetcontrol.msmaintenance.models.enums.MaintenanceType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Representation of a maintenance order. */
public record OrderResponse(
    UUID id,
    UUID planId,
    UUID vehicleId,
    MaintenanceType type,
    MaintenanceOrderStatus status,
    LocalDate scheduledFor,
    Instant startedAt,
    Instant completedAt,
    Integer odometerKm,
    BigDecimal cost,
    String workshop,
    String notes) {}
