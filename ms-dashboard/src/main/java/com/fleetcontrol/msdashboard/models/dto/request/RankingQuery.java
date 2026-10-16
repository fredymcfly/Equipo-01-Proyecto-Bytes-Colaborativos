package com.fleetcontrol.msdashboard.models.dto.request;

import com.fleetcontrol.msdashboard.models.enums.RankingMetric;
import com.fleetcontrol.msdashboard.models.enums.VehicleType;

/**
 * Parameters of the vehicle ranking. As a record it is also the key of the cache.
 *
 * @param type vehicle type to limit the ranking to, or {@code null} for every type
 */
public record RankingQuery(RankingMetric metric, int days, int limit, VehicleType type) {}
