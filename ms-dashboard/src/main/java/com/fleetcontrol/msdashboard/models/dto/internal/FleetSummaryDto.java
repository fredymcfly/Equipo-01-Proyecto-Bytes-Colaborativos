package com.fleetcontrol.msdashboard.models.dto.internal;

import java.util.Map;

/** Answer of {@code GET /api/vehicles/summary} in ms-vehicles. */
public record FleetSummaryDto(long total, Map<String, Long> byStatus, Map<String, Long> byType) {}
