package com.fleetcontrol.msdashboard.models.dto.response;

import com.fleetcontrol.msdashboard.models.enums.DashboardMetric;
import java.util.List;

/** Time series of one metric. */
public record MetricSeriesResponse(
    DashboardMetric metric, String unit, List<SeriesPointResponse> points) {}
