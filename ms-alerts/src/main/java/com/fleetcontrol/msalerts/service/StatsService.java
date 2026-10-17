package com.fleetcontrol.msalerts.service;

import com.fleetcontrol.msalerts.constants.BusinessConstants;
import com.fleetcontrol.msalerts.constants.ErrorConstants;
import com.fleetcontrol.msalerts.exception.InvalidRequestException;
import com.fleetcontrol.msalerts.models.dto.response.StatsResponse;
import com.fleetcontrol.msalerts.models.enums.AlertSeverity;
import com.fleetcontrol.msalerts.models.enums.AlertStatus;
import com.fleetcontrol.msalerts.models.enums.Granularity;
import com.fleetcontrol.msalerts.repository.AlertRepository;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Builds the statistics of the alerts. The periods are in UTC, the empty ones are included with
 * zeros and the total is the sum of the series. The period of a point is its first day: the day
 * itself, the Monday of the week or the first day of the month.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class StatsService {

  private final AlertRepository repository;

  /** Alerts created between the two days, both included, and alerts that are open now. */
  public StatsResponse stats(LocalDate from, LocalDate to, Granularity granularity) {
    validateRange(from, to);
    Instant start = from.atStartOfDay(ZoneOffset.UTC).toInstant();
    Instant end = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

    Map<LocalDate, Map<AlertSeverity, Long>> periods = new LinkedHashMap<>();
    for (LocalDate period = periodStart(from, granularity);
        !period.isAfter(to);
        period = nextPeriod(period, granularity)) {
      periods.put(period, zeroCounts());
    }
    repository
        .findCreatedBetween(start, end)
        .forEach(
            alert -> {
              LocalDate day = alert.getCreatedAt().atOffset(ZoneOffset.UTC).toLocalDate();
              periods
                  .get(periodStart(day, granularity))
                  .merge(alert.getSeverity(), 1L, Long::sum);
            });

    List<StatsResponse.Point> series =
        periods.entrySet().stream()
            .map(
                entry ->
                    new StatsResponse.Point(
                        entry.getKey(), sum(entry.getValue()), entry.getValue()))
            .toList();
    long total = series.stream().mapToLong(StatsResponse.Point::created).sum();

    Map<AlertSeverity, Long> open = zeroCounts();
    repository
        .countBySeverity(AlertStatus.OPEN)
        .forEach(count -> open.put(count.getSeverity(), count.getTotal()));
    return new StatsResponse(
        from, to, granularity, new StatsResponse.Totals(total), series, open);
  }

  private void validateRange(LocalDate from, LocalDate to) {
    if (to.isBefore(from)) {
      throw new InvalidRequestException(
          BusinessConstants.Fields.TO, ErrorConstants.Reason.TO_BEFORE_FROM);
    }
    if (ChronoUnit.DAYS.between(from, to) + 1 > BusinessConstants.Stats.MAX_RANGE_DAYS) {
      throw new InvalidRequestException(
          BusinessConstants.Fields.TO, ErrorConstants.Reason.RANGE_TOO_LARGE);
    }
  }

  private LocalDate periodStart(LocalDate day, Granularity granularity) {
    return switch (granularity) {
      case DAY -> day;
      case WEEK -> day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
      case MONTH -> day.withDayOfMonth(1);
    };
  }

  private LocalDate nextPeriod(LocalDate period, Granularity granularity) {
    return switch (granularity) {
      case DAY -> period.plusDays(1);
      case WEEK -> period.plusWeeks(1);
      case MONTH -> period.plusMonths(1);
    };
  }

  private Map<AlertSeverity, Long> zeroCounts() {
    Map<AlertSeverity, Long> counts = new EnumMap<>(AlertSeverity.class);
    for (AlertSeverity severity : AlertSeverity.values()) {
      counts.put(severity, 0L);
    }
    return counts;
  }

  private long sum(Map<AlertSeverity, Long> counts) {
    return counts.values().stream().mapToLong(Long::longValue).sum();
  }
}
