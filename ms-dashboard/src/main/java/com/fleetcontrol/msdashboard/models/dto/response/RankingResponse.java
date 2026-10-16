package com.fleetcontrol.msdashboard.models.dto.response;

import com.fleetcontrol.msdashboard.models.enums.RankingMetric;
import java.util.List;

/** Vehicles sorted from the highest to the lowest value of the metric. */
public record RankingResponse(
    RankingMetric metric, String unit, PeriodResponse period, List<RankingEntryResponse> ranking) {}
