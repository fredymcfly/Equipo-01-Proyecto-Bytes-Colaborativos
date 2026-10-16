package com.fleetcontrol.msdashboard.models.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

/** Position of a vehicle in the ranking. */
public record RankingEntryResponse(
    int rank, UUID vehicleId, String plate, String type, BigDecimal value) {}
