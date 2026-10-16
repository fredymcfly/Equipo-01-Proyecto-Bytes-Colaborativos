package com.fleetcontrol.msdashboard.models.dto.response;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Pending maintenance order.
 *
 * @param plate plate of the vehicle, or {@code null} when ms-vehicles could not tell it
 */
public record UpcomingMaintenanceResponse(
    UUID orderId, UUID vehicleId, String plate, String type, LocalDate scheduledFor) {}
