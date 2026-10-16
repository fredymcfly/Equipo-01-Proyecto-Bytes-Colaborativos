package com.fleetcontrol.msdashboard.models.dto.request;

import com.fleetcontrol.msdashboard.constants.BusinessConstants;
import com.fleetcontrol.msdashboard.constants.ErrorConstants;
import com.fleetcontrol.msdashboard.exception.InvalidRequestException;
import com.fleetcontrol.msdashboard.models.enums.DashboardMetric;
import com.fleetcontrol.msdashboard.models.enums.Granularity;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Parameters of the time series. It cannot be created with an empty metric list, with a range that
 * ends before it starts or with a range longer than 366 days. As a record it is also the key of the
 * cache.
 */
public record TimeseriesQuery(
    List<DashboardMetric> metrics, LocalDate from, LocalDate to, Granularity granularity) {

  /** Checks the rules of the query. */
  public TimeseriesQuery {
    if (metrics == null || metrics.isEmpty()) {
      throw new InvalidRequestException(
          BusinessConstants.Fields.METRICS, ErrorConstants.Reason.METRICS_REQUIRED);
    }
    if (to.isBefore(from)) {
      throw new InvalidRequestException(
          BusinessConstants.Fields.TO, ErrorConstants.Reason.TO_BEFORE_FROM);
    }
    if (ChronoUnit.DAYS.between(from, to) + 1 > BusinessConstants.Timeseries.MAX_RANGE_DAYS) {
      throw new InvalidRequestException(
          BusinessConstants.Fields.TO, ErrorConstants.Reason.RANGE_TOO_LARGE);
    }
  }

  /** Returns {@code true} when at least one of the metrics is owned by the given service. */
  public boolean requires(String service) {
    return metrics.stream().anyMatch(metric -> metric.getService().equals(service));
  }
}
