package com.fleetcontrol.msalerts.models.dto.internal;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Page of drivers returned by {@code GET /api/drivers} in ms-drivers. */
public record DriversPageDto(List<Driver> content, int totalPages) {

  /** Driver, with the fields the evaluator uses. */
  public record Driver(UUID id, String fullName, LocalDate licenseExpiresAt) {}
}
