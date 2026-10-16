package com.fleetcontrol.msvehicles.models.dto.response;

import com.fleetcontrol.msvehicles.models.enums.VehicleStatus;
import com.fleetcontrol.msvehicles.models.enums.VehicleType;
import java.util.Map;

/** Number of vehicles of the fleet, in total and grouped by status and by type. */
public record FleetSummaryResponse(
    long total, Map<VehicleStatus, Long> byStatus, Map<VehicleType, Long> byType) {}
