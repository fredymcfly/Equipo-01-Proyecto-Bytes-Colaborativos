package com.fleetcontrol.msdashboard.models.dto.response;

import java.time.Instant;
import java.util.List;

/**
 * Executive summary of the fleet. When a source service does not answer, {@code partial} is true,
 * the service is listed in {@code unavailable} and the blocks that depend on it are {@code null}.
 */
public record DashboardResponse(
    Instant generatedAt,
    PeriodResponse period,
    boolean partial,
    List<String> unavailable,
    FleetStatusResponse fleet,
    KpisResponse kpis,
    AlertsSummaryResponse alerts,
    List<UpcomingMaintenanceResponse> upcomingMaintenance) {}
