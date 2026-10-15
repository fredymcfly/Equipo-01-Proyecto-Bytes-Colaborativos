package com.fleetcontrol.msmaintenance.models.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fleetcontrol.msmaintenance.models.enums.MaintenanceType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDate;
import java.util.UUID;

/** Payload used to create a maintenance plan. */
public record CreatePlanRequest(
    @NotNull UUID vehicleId,
    @NotNull MaintenanceType type,
    @Positive Integer intervalKm,
    @Positive Integer intervalDays,
    LocalDate lastDoneAt,
    @PositiveOrZero Integer lastDoneKm) {

  /** Checks that the plan defines at least one interval. */
  @JsonIgnore
  @AssertTrue(message = "intervalKm or intervalDays is required")
  public boolean isIntervalPresent() {
    return intervalKm != null || intervalDays != null;
  }
}
