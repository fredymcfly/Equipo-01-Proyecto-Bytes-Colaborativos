package com.fleetcontrol.msalerts.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fleetcontrol.msalerts.exception.InvalidRequestException;
import com.fleetcontrol.msalerts.models.dto.response.ErrorDetail;
import com.fleetcontrol.msalerts.models.dto.response.StatsResponse;
import com.fleetcontrol.msalerts.models.enums.AlertSeverity;
import com.fleetcontrol.msalerts.models.enums.AlertStatus;
import com.fleetcontrol.msalerts.models.enums.Granularity;
import com.fleetcontrol.msalerts.repository.AlertRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests of the grouping of the alerts by period. */
@ExtendWith(MockitoExtension.class)
class StatsServiceTest {

  @Mock private AlertRepository repository;

  private StatsService service;

  @BeforeEach
  void setUp() {
    service = new StatsService(repository);
  }

  @Test
  void stats_readsTheWholeRangeInUtc() {
    service.stats(LocalDate.of(2026, 9, 7), LocalDate.of(2026, 9, 9), Granularity.DAY);

    verify(repository)
        .findCreatedBetween(
            Instant.parse("2026-09-07T00:00:00Z"), Instant.parse("2026-09-10T00:00:00Z"));
  }

  @Test
  void stats_byDay_fillsTheEmptyDaysWithZeros() {
    when(repository.findCreatedBetween(any(), any()))
        .thenReturn(
            List.of(
                created("2026-09-07T10:00:00Z", AlertSeverity.WARNING),
                created("2026-09-07T22:00:00Z", AlertSeverity.WARNING),
                created("2026-09-09T00:00:00Z", AlertSeverity.CRITICAL)));

    StatsResponse stats =
        service.stats(LocalDate.of(2026, 9, 7), LocalDate.of(2026, 9, 9), Granularity.DAY);

    assertThat(stats.series())
        .extracting(StatsResponse.Point::period, StatsResponse.Point::created)
        .containsExactly(
            tuple(LocalDate.of(2026, 9, 7), 2L),
            tuple(LocalDate.of(2026, 9, 8), 0L),
            tuple(LocalDate.of(2026, 9, 9), 1L));
    assertThat(stats.series().get(1).bySeverity())
        .containsExactly(
            Map.entry(AlertSeverity.INFO, 0L),
            Map.entry(AlertSeverity.WARNING, 0L),
            Map.entry(AlertSeverity.CRITICAL, 0L));
    assertThat(stats.series().get(0).bySeverity().get(AlertSeverity.WARNING)).isEqualTo(2L);
    assertThat(stats.totals().created()).isEqualTo(3);
  }

  @Test
  void stats_byWeek_groupsFromMondayAndTotalsMatchTheSeries() {
    when(repository.findCreatedBetween(any(), any()))
        .thenReturn(
            List.of(
                created("2026-09-13T23:59:59Z", AlertSeverity.INFO),
                created("2026-09-14T00:00:00Z", AlertSeverity.WARNING),
                created("2026-09-20T12:00:00Z", AlertSeverity.WARNING)));

    StatsResponse stats =
        service.stats(LocalDate.of(2026, 9, 7), LocalDate.of(2026, 9, 20), Granularity.WEEK);

    assertThat(stats.series()).hasSize(2);
    assertThat(stats.series().get(0).period()).isEqualTo(LocalDate.of(2026, 9, 7));
    assertThat(stats.series().get(0).created()).isEqualTo(1);
    assertThat(stats.series().get(1).period()).isEqualTo(LocalDate.of(2026, 9, 14));
    assertThat(stats.series().get(1).created()).isEqualTo(2);
    assertThat(stats.totals().created())
        .isEqualTo(stats.series().stream().mapToLong(StatsResponse.Point::created).sum());
  }

  @Test
  void stats_byMonth_startsEachPeriodOnTheFirstDay() {
    when(repository.findCreatedBetween(any(), any()))
        .thenReturn(List.of(created("2026-10-02T08:00:00Z", AlertSeverity.CRITICAL)));

    StatsResponse stats =
        service.stats(LocalDate.of(2026, 9, 20), LocalDate.of(2026, 10, 5), Granularity.MONTH);

    assertThat(stats.series())
        .extracting(StatsResponse.Point::period)
        .containsExactly(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 1));
    assertThat(stats.series().get(1).created()).isEqualTo(1);
  }

  @Test
  void stats_listsTheOpenAlertsWithZerosForMissingSeverities() {
    when(repository.countBySeverity(AlertStatus.OPEN))
        .thenReturn(
            List.of(new Count(AlertSeverity.CRITICAL, 2), new Count(AlertSeverity.WARNING, 9)));

    StatsResponse stats =
        service.stats(LocalDate.of(2026, 9, 7), LocalDate.of(2026, 9, 7), Granularity.DAY);

    assertThat(stats.open())
        .containsExactly(
            Map.entry(AlertSeverity.INFO, 0L),
            Map.entry(AlertSeverity.WARNING, 9L),
            Map.entry(AlertSeverity.CRITICAL, 2L));
  }

  @Test
  void stats_withToBeforeFrom_isRejected() {
    assertThatThrownBy(
            () ->
                service.stats(LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 9), Granularity.DAY))
        .isInstanceOfSatisfying(
            InvalidRequestException.class,
            exception ->
                assertThat(exception.details())
                    .containsExactly(new ErrorDetail("to", "must not be before from")));
    verify(repository, never()).findCreatedBetween(any(), any());
  }

  @Test
  void stats_withMoreThan366Days_isRejected() {
    assertThatThrownBy(
            () ->
                service.stats(
                    LocalDate.of(2025, 9, 1), LocalDate.of(2026, 9, 2), Granularity.MONTH))
        .isInstanceOfSatisfying(
            InvalidRequestException.class,
            exception ->
                assertThat(exception.details())
                    .containsExactly(new ErrorDetail("to", "the range must not exceed 366 days")));
  }

  @Test
  void stats_withExactly366Days_isAllowed() {
    StatsResponse stats =
        service.stats(LocalDate.of(2025, 9, 1), LocalDate.of(2026, 9, 1), Granularity.DAY);

    assertThat(stats.series()).hasSize(366);
  }

  private static AlertRepository.CreatedAlert created(String createdAt, AlertSeverity severity) {
    return new Created(Instant.parse(createdAt), severity);
  }

  private record Created(Instant getCreatedAt, AlertSeverity getSeverity)
      implements AlertRepository.CreatedAlert {}

  private record Count(AlertSeverity getSeverity, long getTotal)
      implements AlertRepository.SeverityCount {}
}
