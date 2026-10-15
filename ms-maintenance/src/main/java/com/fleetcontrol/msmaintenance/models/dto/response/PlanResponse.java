package com.fleetcontrol.msmaintenance.models.dto.response;

import com.fleetcontrol.msmaintenance.models.enums.MaintenanceType;
import java.time.LocalDate;
import java.util.UUID;

/** Representation of a maintenance plan. */
public record PlanResponse(
    UUID id,
    UUID vehicleId,
    MaintenanceType type,
    Integer intervalKm,
    Integer intervalDays,
    LocalDate lastDoneAt,
    Integer lastDoneKm,
    LocalDate nextDueAt,
    Integer nextDueKm,
    Boolean active) {}
