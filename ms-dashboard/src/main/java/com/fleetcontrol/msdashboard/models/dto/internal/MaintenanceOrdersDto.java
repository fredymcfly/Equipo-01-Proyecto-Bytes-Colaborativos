package com.fleetcontrol.msdashboard.models.dto.internal;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Page of work orders returned by {@code GET /api/maintenance/orders} in ms-maintenance. */
public record MaintenanceOrdersDto(List<Order> content) {

  /** Work order, with the fields the dashboard uses. */
  public record Order(UUID id, UUID vehicleId, String type, LocalDate scheduledFor) {}
}
