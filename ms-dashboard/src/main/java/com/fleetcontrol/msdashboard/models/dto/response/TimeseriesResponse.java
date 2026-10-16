package com.fleetcontrol.msdashboard.models.dto.response;

import com.fleetcontrol.msdashboard.models.enums.Granularity;
import java.time.LocalDate;
import java.util.List;

/**
 * Time series of the requested metrics. The metrics of a service that does not answer are left out
 * and the service is listed in {@code unavailable}.
 */
public record TimeseriesResponse(
    LocalDate from,
    LocalDate to,
    Granularity granularity,
    boolean partial,
    List<String> unavailable,
    List<MetricSeriesResponse> series) {}
